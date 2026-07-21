package src.realtime;

import src.model.IBoard;
import src.model.Piece;
import src.model.Position;
import src.view.AnimationConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RealTimeArbiter {

    private static final double CELL_LENGTH_METERS = 0.75;
    private static final long JUMP_DURATION_MS = 1000;
    public static final long LONG_REST_MS = 2000;
    public static final long SHORT_REST_MS = 500;

    private final String piecesRoot;
    private final MotionResolver motionResolver;
    private final JumpResolver jumpResolver;
    private final CollisionResolver collisionResolver;
    private final PathCrossingResolver pathCrossingResolver = new PathCrossingResolver();

    private final Map<Piece, Motion> activeMotions = new LinkedHashMap<>();
    private final Map<Piece, Long> motionElapsedMs = new LinkedHashMap<>();

    private final Map<Piece, Position> activeJumps = new LinkedHashMap<>();
    private final Map<Piece, Long> jumpElapsedMs = new LinkedHashMap<>();

    private final Map<Piece, Long> longRestElapsedMs = new LinkedHashMap<>();
    private final Map<Piece, Long> shortRestElapsedMs = new LinkedHashMap<>();

    public RealTimeArbiter(IBoard board) {
        this(board, "assets/pieces");
    }

    public RealTimeArbiter(IBoard board, String piecesRoot) {
        this.piecesRoot = piecesRoot;
        this.motionResolver = new MotionResolver(board);
        this.jumpResolver = new JumpResolver(board);
        this.collisionResolver = new CollisionResolver(board, jumpResolver, motionResolver);
    }

    public boolean isIdle() {
        return activeMotions.isEmpty() && activeJumps.isEmpty()
                && longRestElapsedMs.isEmpty() && shortRestElapsedMs.isEmpty();
    }

    public boolean isMoving(Piece piece) {
        return activeMotions.containsKey(piece);
    }

    public boolean isJumping(Piece piece) {
        return activeJumps.containsKey(piece);
    }

    public Optional<Motion> activeMotion(Piece piece) {
        return Optional.ofNullable(activeMotions.get(piece));
    }

    public long motionElapsedMs(Piece piece) {
        return motionElapsedMs.getOrDefault(piece, 0L);
    }

    public long jumpElapsedMs(Piece piece) {
        return jumpElapsedMs.getOrDefault(piece, 0L);
    }

    public boolean isResting(Piece piece) {
        return isLongResting(piece) || isShortResting(piece);
    }

    public boolean isLongResting(Piece piece) {
        return longRestElapsedMs.containsKey(piece);
    }

    public boolean isShortResting(Piece piece) {
        return shortRestElapsedMs.containsKey(piece);
    }

    public long longRestElapsedMs(Piece piece) {
        return longRestElapsedMs.getOrDefault(piece, 0L);
    }

    public long shortRestElapsedMs(Piece piece) {
        return shortRestElapsedMs.getOrDefault(piece, 0L);
    }

    public void startMotion(Piece piece, Position source, Position destination) {
        if (activeMotions.containsKey(piece)) {
            throw new IllegalStateException("this piece already has a motion in progress");
        }

        long distance = Math.max(
                Math.abs(source.row() - destination.row()),
                Math.abs(source.col() - destination.col()));

        double speedMetersPerSecond = moveConfigFor(piece).speedMetersPerSecond();
        long durationMs = speedMetersPerSecond > 0
                ? Math.round(distance * CELL_LENGTH_METERS / speedMetersPerSecond * 1000)
                : 0;

        piece.setState(Piece.State.MOVING);
        activeMotions.put(piece, new Motion(piece, source, destination, durationMs));
        motionElapsedMs.put(piece, 0L);
    }

    public void startJump(Piece piece, Position cell) {
        if (piece.state() == Piece.State.MOVING || isResting(piece) || activeJumps.containsKey(piece)) {
            return;
        }
        piece.setState(Piece.State.JUMPING);
        activeJumps.put(piece, cell);
        jumpElapsedMs.put(piece, 0L);
    }

    public List<ArrivalEvent> advanceTime(long ms) {
        for (Piece piece : activeMotions.keySet()) {
            motionElapsedMs.merge(piece, ms, Long::sum);
        }
        for (Piece piece : activeJumps.keySet()) {
            jumpElapsedMs.merge(piece, ms, Long::sum);
        }
        for (Piece piece : longRestElapsedMs.keySet()) {
            longRestElapsedMs.merge(piece, ms, Long::sum);
        }
        for (Piece piece : shortRestElapsedMs.keySet()) {
            shortRestElapsedMs.merge(piece, ms, Long::sum);
        }

        activeMotions.putAll(pathCrossingResolver.truncateLaterArrivals(activeMotions, motionElapsedMs));

        List<Piece> duePieces = dueMotionPieces();
        List<Piece> dueJumpers = dueJumpPieces();

        List<ArrivalEvent> events = new ArrayList<>();

        for (Piece jumper : dueJumpers) {
            resolveJumpTick(jumper, duePieces).ifPresent(events::add);
        }

        events.addAll(resolveMotionsForTick(duePieces));

        resolveRestTicks(longRestElapsedMs, LONG_REST_MS);
        resolveRestTicks(shortRestElapsedMs, SHORT_REST_MS);

        return events;
    }

    private void resolveRestTicks(Map<Piece, Long> elapsedByPiece, long durationMs) {
        List<Piece> duePieces = new ArrayList<>();
        for (Map.Entry<Piece, Long> entry : elapsedByPiece.entrySet()) {
            if (entry.getValue() >= durationMs) {
                duePieces.add(entry.getKey());
            }
        }
        for (Piece piece : duePieces) {
            elapsedByPiece.remove(piece);
            if (piece.state() != Piece.State.CAPTURED) {
                piece.setState(Piece.State.IDLE);
            }
        }
    }

    private List<Piece> dueMotionPieces() {
        List<Piece> duePieces = new ArrayList<>();
        for (Map.Entry<Piece, Motion> entry : activeMotions.entrySet()) {
            if (motionElapsedMs.get(entry.getKey()) >= entry.getValue().durationMs()) {
                duePieces.add(entry.getKey());
            }
        }
        return duePieces;
    }

    private List<Piece> dueJumpPieces() {
        List<Piece> duePieces = new ArrayList<>();
        for (Map.Entry<Piece, Position> entry : activeJumps.entrySet()) {
            if (jumpElapsedMs.get(entry.getKey()) >= JUMP_DURATION_MS) {
                duePieces.add(entry.getKey());
            }
        }
        return duePieces;
    }

    private Optional<ArrivalEvent> resolveJumpTick(Piece defender, List<Piece> dueMotionPieces) {
        Position cell = activeJumps.remove(defender);
        jumpElapsedMs.remove(defender);

        Optional<Piece> collidingAttacker = collisionResolver.findMotionLandingOnJumpCell(cell, dueMotionPieces, activeMotions);
        Optional<ArrivalEvent> event;
        if (collidingAttacker.isPresent()) {
            Motion attackerMotion = takeMotion(collidingAttacker.get());
            event = Optional.of(collisionResolver.resolveJumpCountersAttackingMotion(defender, cell, attackerMotion));
        } else {
            event = jumpResolver.resolveLanding(defender, cell);
        }

        if (defender.state() != Piece.State.CAPTURED) {
            startConfiguredRest(defender, jumpConfigFor(defender).nextStateWhenFinished());
        }
        return event;
    }

   
    private List<ArrivalEvent> resolveMotionsForTick(List<Piece> duePieces) {
        List<ArrivalEvent> events = new ArrayList<>();
        List<Piece> livePieces = new ArrayList<>();

        for (Piece piece : duePieces) {
            if (!activeMotions.containsKey(piece)) {
                continue;
            }
            Motion motion = activeMotions.get(piece);

            if (collisionResolver.isMotionLandingOnAirbornePiece(motion, activeJumps.keySet())) {
                takeMotion(piece);
                collisionResolver.clearCellForDisplacement(motion.destination());
                ArrivalEvent event = motionResolver.resolveWithoutCapture(motion);
                applyRestIfArrived(event);
                events.add(event);
                continue;
            }

            livePieces.add(piece);
        }

        Map<Position, List<Piece>> competitorsByDestination = new LinkedHashMap<>();
        for (Piece piece : livePieces) {
            Position destination = activeMotions.get(piece).destination();
            competitorsByDestination.computeIfAbsent(destination, unused -> new ArrayList<>()).add(piece);
        }

        for (List<Piece> competitors : competitorsByDestination.values()) {
            List<Piece> stillLive = new ArrayList<>();
            for (Piece candidate : competitors) {
                Motion motion = activeMotions.get(candidate);
                if (collisionResolver.isPieceAlreadyCaptured(motion)) {
                    takeMotion(candidate);
                } else {
                    stillLive.add(candidate);
                }
            }

            if (stillLive.isEmpty()) {
                continue;
            }
            if (stillLive.size() == 1) {
                Motion motion = takeMotion(stillLive.get(0));
                ArrivalEvent event = motionResolver.resolve(motion);
                applyRestIfArrived(event);
                events.add(event);
                continue;
            }

            Piece winner = collisionResolver.pickRaceWinner(stillLive, activeMotions, motionElapsedMs);
            Motion winnerMotion = takeMotion(winner);
            ArrivalEvent winnerEvent = motionResolver.resolve(winnerMotion);
            applyRestIfArrived(winnerEvent);
            events.add(winnerEvent);

            boolean winnerActuallyArrived = !winnerEvent.to().equals(winnerEvent.from());

            for (Piece loser : stillLive) {
                if (loser == winner) {
                    continue;
                }
                Motion loserMotion = takeMotion(loser);
                events.add(winnerActuallyArrived
                        ? collisionResolver.resolveRaceLoserAgainstWinner(winner, winnerMotion, loserMotion)
                        : motionResolver.resolveBounceBack(loserMotion));
            }
        }

        return events;
    }

    private Motion takeMotion(Piece piece) {
        motionElapsedMs.remove(piece);
        return activeMotions.remove(piece);
    }

    private void applyRestIfArrived(ArrivalEvent event) {
        Piece piece = event.movedPiece();
        if (piece.state() == Piece.State.CAPTURED) {
            return;
        }
        if (event.to().equals(event.from())) {
            return;
        }
        startConfiguredRest(piece, moveConfigFor(piece).nextStateWhenFinished());
    }

    private void startConfiguredRest(Piece piece, String nextState) {
        if ("long_rest".equals(nextState)) {
            longRestElapsedMs.put(piece, 0L);
            piece.setState(Piece.State.LONG_REST);
        } else if ("short_rest".equals(nextState)) {
            shortRestElapsedMs.put(piece, 0L);
            piece.setState(Piece.State.SHORT_REST);
        }
       
    }

    private AnimationConfig moveConfigFor(Piece piece) {
        return AnimationConfig.load(configPathFor(piece, "move"));
    }

    private AnimationConfig jumpConfigFor(Piece piece) {
        return AnimationConfig.load(configPathFor(piece, "jump"));
    }

    private String configPathFor(Piece piece, String stateFolder) {
        return piecesRoot + "/" + piece.kind().letter() + piece.color().letter()
                + "/states/" + stateFolder + "/config.json";
    }
}
