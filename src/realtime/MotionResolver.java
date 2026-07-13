package src.realtime;

import src.model.Board;
import src.model.Piece;
import src.model.Position;

public class MotionResolver {

    private final Board board;

    public MotionResolver(Board board) {
        this.board = board;
    }

    public ArrivalEvent resolve(Motion motion) {
        Piece capturedPiece = board.getPieceAt(motion.destination()).orElse(null);

        if (capturedPiece != null && capturedPiece.getColor() == motion.piece().getColor()) {
            // The destination became friendly-occupied after this motion started (the board
            // only changes on arrival, so RuleEngine could not have foreseen this at request
            // time). This is not a capture: the motion is cancelled and the piece bounces back
            // to idle at its own source without ever actually moving.
            return resolveBounceBack(motion);
        }

        boolean kingCaptured = capturedPiece != null && capturedPiece.getKind() == Piece.Kind.KING;
        if (capturedPiece != null) {
            capturedPiece.setState(Piece.State.CAPTURED);
            board.removePiece(motion.destination());
        }

        board.movePiece(motion.source(), motion.destination());
        Piece arrivedPiece = arriveAndMaybePromote(motion);

        return new ArrivalEvent(arrivedPiece, motion.source(), motion.destination(), capturedPiece, kingCaptured);
    }

    public ArrivalEvent resolveBounceBack(Motion motion) {
        Piece piece = motion.piece();
        piece.setState(Piece.State.IDLE);
        // to == from: the piece never actually relocated, it just cancelled its motion in place.
        return new ArrivalEvent(piece, motion.source(), motion.source(), null, false);
    }

    public ArrivalEvent resolveWithoutCapture(Motion motion) {
        board.movePiece(motion.source(), motion.destination());
        Piece arrivedPiece = arriveAndMaybePromote(motion);

        return new ArrivalEvent(arrivedPiece, motion.source(), motion.destination(), null, false);
    }

    private Piece arriveAndMaybePromote(Motion motion) {
        Piece arrivedPiece = motion.piece();
        arrivedPiece.setState(Piece.State.IDLE);
        if (isPromotion(arrivedPiece)) {
            arrivedPiece = promoteToQueen(arrivedPiece);
        }
        return arrivedPiece;
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
