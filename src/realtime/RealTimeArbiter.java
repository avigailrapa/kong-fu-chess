package src.realtime;

import src.model.Board;
import src.model.Piece;
import src.model.Position;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RealTimeArbiter {

    private static final long MILLIS_PER_CELL = 1000;
    private static final long JUMP_DURATION_MS = 1000;

    private final MotionResolver motionResolver;
    private final JumpResolver jumpResolver;
    private final CollisionResolver collisionResolver;

    private final Map<Piece, Motion> activeMotions = new LinkedHashMap<>();
    private final Map<Piece, Long> motionElapsedMs = new LinkedHashMap<>();

    private final Map<Piece, Position> activeJumps = new LinkedHashMap<>();
    private final Map<Piece, Long> jumpElapsedMs = new LinkedHashMap<>();

    public RealTimeArbiter(Board board) {
        this.motionResolver = new MotionResolver(board);
        this.jumpResolver = new JumpResolver(board);
        this.collisionResolver = new CollisionResolver(board, jumpResolver, motionResolver);
    }

    public boolean hasActiveMotion() {
        return !activeMotions.isEmpty();
    }

    public boolean isMoving(Piece piece) {
        return activeMotions.containsKey(piece);
    }

    public boolean hasActiveJump() {
        return !activeJumps.isEmpty();
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

    public void startMotion(Piece piece, Position source, Position destination) {
        if (activeMotions.containsKey(piece)) {
            throw new IllegalStateException("this piece already has a motion in progress");
        }

        long distance = Math.max(
                Math.abs(source.getRow() - destination.getRow()),
                Math.abs(source.getCol() - destination.getCol()));

        piece.setState(Piece.State.MOVING);
        activeMotions.put(piece, new Motion(piece, source, destination, distance * MILLIS_PER_CELL));
        motionElapsedMs.put(piece, 0L);
    }

    public void startJump(Piece piece, Position cell) {
        if (piece.getState() == Piece.State.MOVING || activeJumps.containsKey(piece)) {
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

        List<Piece> duePieces = dueMotionPieces();
        List<Piece> dueJumpers = dueJumpPieces();

        List<ArrivalEvent> events = new ArrayList<>();

        for (Piece jumper : dueJumpers) {
            resolveJumpTick(jumper, duePieces).ifPresent(events::add);
        }

        events.addAll(resolveMotionsForTick(duePieces));

        return events;
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
        if (collidingAttacker.isPresent()) {
            Motion attackerMotion = takeMotion(collidingAttacker.get());
            return Optional.of(collisionResolver.resolveJumpCountersAttackingMotion(defender, cell, attackerMotion));
        }

        return jumpResolver.resolveLanding(defender, cell);
    }

    // Due motions are grouped by destination cell before any of them touch the board, so a
    // same-tick race is decided by pickRaceWinner rather than by map iteration order.
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
                events.add(motionResolver.resolveWithoutCapture(motion));
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
                events.add(motionResolver.resolve(motion));
                continue;
            }

            Piece winner = collisionResolver.pickRaceWinner(stillLive, activeMotions, motionElapsedMs);
            Motion winnerMotion = takeMotion(winner);
            events.add(motionResolver.resolve(winnerMotion));

            for (Piece loser : stillLive) {
                if (loser == winner) {
                    continue;
                }
                Motion loserMotion = takeMotion(loser);
                events.add(collisionResolver.resolveRaceLoserAgainstWinner(winner, winnerMotion, loserMotion));
            }
        }

        return events;
    }

    private Motion takeMotion(Piece piece) {
        motionElapsedMs.remove(piece);
        return activeMotions.remove(piece);
    }
}
