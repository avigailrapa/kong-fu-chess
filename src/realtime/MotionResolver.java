package src.realtime;

import src.model.IBoard;
import src.model.Piece;
import src.model.Position;

public class MotionResolver {

    private final IBoard board;

    public MotionResolver(IBoard board) {
        this.board = board;
    }

    public ArrivalEvent resolve(Motion motion) {
        Piece capturedPiece = board.getPieceAt(motion.destination()).orElse(null);

        if (capturedPiece != null && capturedPiece.color() == motion.piece().color()) {
            return resolveBounceBack(motion);
        }

        boolean kingCaptured = capturedPiece != null && capturedPiece.kind() == Piece.Kind.KING;
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
        Position stoppedAt = motion.cellBeforeDestination();
        if (!stoppedAt.equals(motion.source())) {
            board.movePiece(motion.source(), stoppedAt);
        }
        piece.setState(Piece.State.IDLE);
        return new ArrivalEvent(piece, motion.source(), stoppedAt, null, false);
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
        if (piece.kind() != Piece.Kind.PAWN) {
            return false;
        }
        int promotionRow = piece.color() == Piece.Color.WHITE ? 0 : board.getHeight() - 1;
        return piece.cell().row() == promotionRow;
    }

    private Piece promoteToQueen(Piece pawn) {
        Position cell = pawn.cell();
        board.removePiece(cell);
        Piece queen = new Piece(pawn.id(), pawn.color(), Piece.Kind.QUEEN, cell);
        board.addPiece(queen, cell);
        return queen;
    }
}
