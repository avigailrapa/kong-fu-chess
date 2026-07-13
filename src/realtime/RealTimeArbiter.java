package src.realtime;

import src.model.Board;
import src.model.Piece;
import src.model.Position;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class RealTimeArbiter {

    private static final long MILLIS_PER_CELL = 1000;
    private static final long JUMP_DURATION_MS = 1000;
    private static final long DEFAULT_COOLDOWN_MS = 1000;

    private static final Map<Piece.Kind, Long> COOLDOWN_MS_BY_KIND = new EnumMap<>(Piece.Kind.class);
    static {
        COOLDOWN_MS_BY_KIND.put(Piece.Kind.PAWN, 1000L);
        COOLDOWN_MS_BY_KIND.put(Piece.Kind.KNIGHT, 1000L);
        COOLDOWN_MS_BY_KIND.put(Piece.Kind.BISHOP, 1000L);
        COOLDOWN_MS_BY_KIND.put(Piece.Kind.ROOK, 1000L);
        COOLDOWN_MS_BY_KIND.put(Piece.Kind.QUEEN, 1000L);
        COOLDOWN_MS_BY_KIND.put(Piece.Kind.KING, 1000L);
    }

    private final Board board;
    private Motion activeMotion;
    private long elapsedMs;

    private Piece jumpingPiece;
    private Position jumpCell;
    private long jumpElapsedMs;

    private final Map<Piece, Long> cooldownRemainingMs = new HashMap<>();

    public RealTimeArbiter(Board board) {
        this.board = board;
    }

    public boolean hasActiveMotion() {
        return activeMotion != null;
    }

    public boolean hasActiveJump() {
        return jumpingPiece != null;
    }

    public boolean isOnCooldown(Piece piece) {
        return cooldownRemainingMs.containsKey(piece);
    }

    public void startMotion(Piece piece, Position source, Position destination) {
        if (activeMotion != null) {
            throw new IllegalStateException("a motion is already in progress");
        }

        long distance = Math.max(
                Math.abs(source.getRow() - destination.getRow()),
                Math.abs(source.getCol() - destination.getCol()));

        piece.setState(Piece.State.MOVING);
        activeMotion = new Motion(piece, source, destination, distance * MILLIS_PER_CELL);
        elapsedMs = 0;
    }

    public void startJump(Piece piece, Position cell) {
        if (piece.getState() == Piece.State.MOVING || jumpingPiece != null) {
            return;
        }
        jumpingPiece = piece;
        jumpCell = cell;
        jumpElapsedMs = 0;
    }

    public Optional<ArrivalEvent> advanceTime(long ms) {
        tickCooldowns(ms);

        if (activeMotion != null) {
            elapsedMs += ms;
        }
        if (jumpingPiece != null) {
            jumpElapsedMs += ms;
        }

        boolean motionDue = activeMotion != null && elapsedMs >= activeMotion.durationMs();
        boolean jumpDue = jumpingPiece != null && jumpElapsedMs >= JUMP_DURATION_MS;

       
        if (jumpDue) {
            return resolveJumpLanding(motionDue);
        }
        
        if (motionDue) {
            return resolveMotionArrival();
        }
        
        return Optional.empty();
    }

   
    private Optional<ArrivalEvent> resolveJumpLanding(boolean motionAlsoDue) {
        Piece defender = jumpingPiece;
        Position cell = jumpCell;
        
        jumpingPiece = null;
        jumpCell = null;
        jumpElapsedMs = 0;

        
        if (motionAlsoDue && activeMotion.destination().equals(cell)) {
            Motion motion = activeMotion;
            activeMotion = null;
            elapsedMs = 0;

            Piece attacker = motion.piece();
            boolean attackerWasKing = attacker.getKind() == Piece.Kind.KING;
            attacker.setState(Piece.State.CAPTURED);

            board.removePiece(motion.source());

            defender.setState(Piece.State.IDLE);
            return Optional.of(new ArrivalEvent(defender, cell, cell, attacker, attackerWasKing));
        }

        Piece occupant = board.getPieceAt(cell).orElse(null);

        if (occupant == defender) {
            defender.setState(Piece.State.IDLE);
            return Optional.empty();
        }

     
        defender.setState(Piece.State.CAPTURED);
        boolean defenderWasKing = defender.getKind() == Piece.Kind.KING;
        return Optional.of(new ArrivalEvent(defender, cell, cell, occupant, defenderWasKing));
    }

  
    private Optional<ArrivalEvent> resolveMotionArrival() {
        Motion motion = activeMotion;
        activeMotion = null;
        elapsedMs = 0;

        Piece capturedPiece = board.getPieceAt(motion.destination()).orElse(null);

        
        if (jumpingPiece != null && capturedPiece == jumpingPiece) {

            board.removePiece(motion.destination());
            board.movePiece(motion.source(), motion.destination());
            Piece arrivedPiece = motion.piece();
            arrivedPiece.setState(Piece.State.IDLE);
            if (isPromotion(arrivedPiece)) {
                arrivedPiece = promoteToQueen(arrivedPiece);
            }
            startCooldown(arrivedPiece);
            return Optional.of(new ArrivalEvent(arrivedPiece, motion.source(), motion.destination(), null, false));
        }

        boolean kingCaptured = capturedPiece != null && capturedPiece.getKind() == Piece.Kind.KING;
        if (capturedPiece != null) {
            capturedPiece.setState(Piece.State.CAPTURED);
            board.removePiece(motion.destination());
        }

        board.movePiece(motion.source(), motion.destination());
        Piece arrivedPiece = motion.piece();
        arrivedPiece.setState(Piece.State.IDLE);

        if (isPromotion(arrivedPiece)) {
            arrivedPiece = promoteToQueen(arrivedPiece);
        }
        startCooldown(arrivedPiece);

        return Optional.of(new ArrivalEvent(arrivedPiece, motion.source(), motion.destination(), capturedPiece, kingCaptured));
    }

    private void tickCooldowns(long ms) {
        Iterator<Map.Entry<Piece, Long>> it = cooldownRemainingMs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Piece, Long> entry = it.next();
            long remaining = entry.getValue() - ms;
            if (remaining <= 0) {
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    private void startCooldown(Piece piece) {
        long duration = COOLDOWN_MS_BY_KIND.getOrDefault(piece.getKind(), DEFAULT_COOLDOWN_MS);
        cooldownRemainingMs.put(piece, duration);
    }

    private boolean isPromotion(Piece piece) {
        if (piece.getKind() != Piece.Kind.PAWN) {
            return false;
        }
        int promotionRow = piece.getColor() == Piece.Color.WHITE ? 0 : board.getHeight() - 1;
        return piece.getCell().getRow() == promotionRow;
    }

    private Piece promoteToQueen(Piece pawn) {
        Position cell = pawn.getCell();
        board.removePiece(cell);
        Piece queen = new Piece(pawn.getId(), pawn.getColor(), Piece.Kind.QUEEN, cell);
        board.addPiece(queen, cell);
        return queen;
    }
}