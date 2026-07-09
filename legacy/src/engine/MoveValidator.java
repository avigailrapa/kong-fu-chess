import model.*;
import model.IBoard;
public class MoveValidator {
    private final IBoard board;

    public MoveValidator(IBoard board) {
        this.board = board;
    }

    public boolean isValidMove(char pieceType, char pieceColor, int startRow, int startCol, 
                               int targetRow, int targetCol) {
        if (!isValidShape(pieceType, pieceColor, startRow, startCol, targetRow, targetCol)) {
            return false;
        }
        return isPathClear(pieceType, startRow, startCol, targetRow, targetCol);
    }

    private boolean isValidShape(char pieceType, char pieceColor, int startRow, int startCol, 
                                 int targetRow, int targetCol) {
        int dx = Math.abs(targetCol - startCol);
        int dy = Math.abs(targetRow - startRow);

        if (dx == 0 && dy == 0) return false;

        if (pieceType == Piece.KING) {
            return dx <= 1 && dy <= 1;
        } else if (pieceType == Piece.ROOK) {
            return dx == 0 || dy == 0;
        } else if (pieceType == Piece.BISHOP) {
            return dx == dy;
        } else if (pieceType == Piece.QUEEN) {
            return dx == dy || dx == 0 || dy == 0;
        } else if (pieceType == Piece.KNIGHT) {
            return (dx == 1 && dy == 2) || (dx == 2 && dy == 1);
        } else if (pieceType == Piece.PAWN) {
            return isValidPawnShape(pieceColor, startRow, startCol, targetRow, targetCol);
        }
        return false;
    }

    private boolean isValidPawnShape(char pieceColor, int startRow, int startCol, int targetRow, int targetCol) {
        int dx = Math.abs(targetCol - startCol);
        int rowDiff = targetRow - startRow;
        
        // כיוון התנועה נקבע דינמית לפי צבע הכלי (מונע כפל קוד)
        int direction = (pieceColor == Piece.WHITE) ? -1 : 1;
        int initialRow = (pieceColor == Piece.WHITE) ? board.getNumRows() - 2 : 1;

        String targetCell = board.getPieceAt(targetRow, targetCol);

        if (dx == 0) {
            if (rowDiff == direction) {
                return targetCell.equals(Piece.EMPTY);
            } else if (rowDiff == 2 * direction && startRow == initialRow) {
                return targetCell.equals(Piece.EMPTY);
            }
        } else if (dx == 1 && rowDiff == direction) {
            return !targetCell.equals(Piece.EMPTY);
        }
        return false;
    }

    private boolean isPathClear(char pieceType, int startRow, int startCol, int targetRow, int targetCol) {
        if (pieceType == Piece.KNIGHT || pieceType == Piece.KING) {
            return true;
        }

        if (pieceType == Piece.PAWN) {
            int dy = Math.abs(targetRow - startRow);
            int dx = Math.abs(targetCol - startCol);
            if (dx == 0 && dy == 2) {
                int midRow = (startRow + targetRow) / 2;
                return board.getPieceAt(midRow, startCol).equals(Piece.EMPTY);
            }
            return true;
        }

        int rowStep = Integer.compare(targetRow, startRow);
        int colStep = Integer.compare(targetCol, startCol);

        int currentRow = startRow + rowStep;
        int currentCol = startCol + colStep;

        while (currentRow != targetRow || currentCol != targetCol) {
            if (!board.getPieceAt(currentRow, currentCol).equals(Piece.EMPTY)) {
                return false;
            }
            currentRow += rowStep;
            currentCol += colStep;
        }

        return true;
    }
}