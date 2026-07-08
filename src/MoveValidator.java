/**
 * Validates moves according to chess piece rules.
 * SRP: Single responsibility - move validation only.
 * DRY: All move validation logic in one place.
 */
public class MoveValidator {
    private final Board board;

    public MoveValidator(Board board) {
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

        switch (pieceType) {
            case Piece.KING:
                return dx <= 1 && dy <= 1;
            case Piece.ROOK:
                return dx == 0 || dy == 0;
            case Piece.BISHOP:
                return dx == dy;
            case Piece.QUEEN:
                return dx == dy || dx == 0 || dy == 0;
            case Piece.KNIGHT:
                return (dx == 1 && dy == 2) || (dx == 2 && dy == 1);
            case Piece.PAWN:
                return isValidPawnMove(pieceColor, startRow, startCol, targetRow, targetCol);
            default:
                return false;
        }
    }

    private boolean isValidPawnMove(char pieceColor, int startRow, int startCol, 
                                    int targetRow, int targetCol) {
        int dx = Math.abs(targetCol - startCol);
        int dy = Math.abs(targetRow - startRow);
        String targetCell = board.getPieceAt(targetRow, targetCol);

        if (pieceColor == Piece.WHITE) {
            int rowDiff = startRow - targetRow;
            if (dx == 0) {
                if (rowDiff == 1) return targetCell.equals(Piece.EMPTY);
                if (rowDiff == 2 && startRow == board.getNumRows() - 1) {
                    return targetCell.equals(Piece.EMPTY);
                }
                return false;
            } else if (dx == 1 && rowDiff == 1) {
                return !targetCell.equals(Piece.EMPTY);
            }
            return false;
        } else {
            int rowDiff = targetRow - startRow;
            if (dx == 0) {
                if (rowDiff == 1) return targetCell.equals(Piece.EMPTY);
                if (rowDiff == 2 && startRow == 0) return targetCell.equals(Piece.EMPTY);
                return false;
            } else if (dx == 1 && rowDiff == 1) {
                return !targetCell.equals(Piece.EMPTY);
            }
            return false;
        }
    }

    private boolean isPathClear(char pieceType, int startRow, int startCol, 
                               int targetRow, int targetCol) {
        if (pieceType == Piece.KNIGHT || pieceType == Piece.KING) {
            return true;
        }

        if (pieceType == Piece.PAWN) {
            return isPawnPathClear(startRow, startCol, targetRow, targetCol);
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

    private boolean isPawnPathClear(int startRow, int startCol, int targetRow, int targetCol) {
        int dy = Math.abs(targetRow - startRow);
        int dx = Math.abs(targetCol - startCol);
        if (dx == 0 && dy == 2) {
            int midRow = (startRow + targetRow) / 2;
            return board.getPieceAt(midRow, startCol).equals(Piece.EMPTY);
        }
        return true;
    }
}
