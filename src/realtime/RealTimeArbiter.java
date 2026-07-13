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
        this.collisionResolver = new CollisionResolver(board, jumpResolver);
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

        for (Piece piece : duePieces) {
            if (!activeMotions.containsKey(piece)) {
                continue;
            }
            resolveMotionTick(piece).ifPresent(events::add);
        }

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

    private Optional<ArrivalEvent> resolveMotionTick(Piece piece) {
        Motion motion = takeMotion(piece);

        if (collisionResolver.isPieceAlreadyCaptured(motion)) {
            return Optional.empty();
        }

        if (collisionResolver.isMotionLandingOnAirbornePiece(motion, activeJumps.keySet())) {
            collisionResolver.clearCellForDisplacement(motion.destination());
            return Optional.of(motionResolver.resolveWithoutCapture(motion));
        }

        return Optional.of(motionResolver.resolve(motion));
    }

    private Motion takeMotion(Piece piece) {
        motionElapsedMs.remove(piece);
        return activeMotions.remove(piece);
    }
}
