package model;


public class JumpState {
    private final int row;
    private final int col;
    private final long startTime;
    private final long endTime;

    public JumpState(int row, int col, long startTime, long endTime) {
        if (endTime < startTime) {
            throw new IllegalArgumentException("End time must be >= start time");
        }
        this.row = row;
        this.col = col;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public boolean isAtLocation(int row, int col) {
        return this.row == row && this.col == col;
    }

    public boolean isActiveAt(long currentTime) {
        return currentTime >= startTime && currentTime <= endTime;
    }

    public boolean hasLanded(long currentTime) {
        return currentTime > endTime;
    }
}
