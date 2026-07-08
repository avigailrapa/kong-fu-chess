/**
 * Represents a move that is currently in progress.
 * SRP: Encapsulates movement state and timing.
 */
public class PendingMove {
    private final int startRow;
    private final int startCol;
    private final int targetRow;
    private final int targetCol;
    private final String piece;
    private final long arrivalTime;

    public PendingMove(int startRow, int startCol, int targetRow, int targetCol, String piece, long arrivalTime) {
        if (arrivalTime < 0) {
            throw new IllegalArgumentException("Arrival time cannot be negative");
        }
        this.startRow = startRow;
        this.startCol = startCol;
        this.targetRow = targetRow;
        this.targetCol = targetCol;
        this.piece = piece;
        this.arrivalTime = arrivalTime;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getStartCol() {
        return startCol;
    }

    public int getTargetRow() {
        return targetRow;
    }

    public int getTargetCol() {
        return targetCol;
    }

    public String getPiece() {
        return piece;
    }

    public long getArrivalTime() {
        return arrivalTime;
    }

    public boolean hasArrived(long currentTime) {
        return currentTime >= arrivalTime;
    }

    public boolean originatesFrom(int row, int col) {
        return startRow == row && startCol == col;
    }

    public boolean targetsCell(int row, int col) {
        return targetRow == row && targetCol == col;
    }
}
