package realtime;

import model.Board;
import model.Piece;
import model.Position;

import java.util.Optional;

public class RealTimeArbiter {

    private static final long MILLIS_PER_CELL = 1000;

    private final Board board;
    private Motion activeMotion;

    public RealTimeArbiter(Board board) {
        this.board = board;
    }

    public boolean hasActiveMotion() {
        return activeMotion != null;
    }

    public void startMotion(Position source, Position destination, long currentTimeMs) {
        if (activeMotion != null) {
            throw new IllegalStateException("a motion is already in progress");
        }
        Piece piece = board.getPieceAt(source)
                .orElseThrow(() -> new IllegalStateException("no piece at source: " + source));

        long distance = Math.max(
                Math.abs(source.getRow() - destination.getRow()),
                Math.abs(source.getCol() - destination.getCol()));

        piece.setState(Piece.State.MOVING);
        activeMotion = new Motion(piece, source, destination, currentTimeMs, distance * MILLIS_PER_CELL);
    }

    public Optional<ArrivalEvent> advanceTime(long currentTimeMs) {
        if (activeMotion == null || currentTimeMs < activeMotion.arrivalTimeMs()) {
            return Optional.empty();
        }

        Motion motion = activeMotion;
        activeMotion = null;

        Piece capturedPiece = board.getPieceAt(motion.destination()).orElse(null);
        boolean kingCaptured = capturedPiece != null && capturedPiece.getKind() == Piece.Kind.KING;
        if (capturedPiece != null) {
            capturedPiece.setState(Piece.State.CAPTURED);
            board.removePiece(motion.destination());
        }

        board.movePiece(motion.source(), motion.destination());
        motion.piece().setState(Piece.State.IDLE);

        return Optional.of(new ArrivalEvent(motion.piece(), motion.source(), motion.destination(), capturedPiece, kingCaptured));
    }
}
