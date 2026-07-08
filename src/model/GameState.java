package features;

import java.util.ArrayList;
import java.util.List;


public class GameState {
    private int selectedRow;
    private int selectedCol;
    private boolean gameOver;
    private final List<PendingMove> pendingMoves;
    private final List<JumpState> activeJumps;

    public GameState() {
        this.selectedRow = -1;
        this.selectedCol = -1;
        this.gameOver = false;
        this.pendingMoves = new ArrayList<>();
        this.activeJumps = new ArrayList<>();
    }

    // Selection management
    public void selectPiece(int row, int col) {
        this.selectedRow = row;
        this.selectedCol = col;
    }

    public void clearSelection() {
        this.selectedRow = -1;
        this.selectedCol = -1;
    }

    public boolean hasSelection() {
        return selectedRow != -1 && selectedCol != -1;
    }

    public int getSelectedRow() {
        return selectedRow;
    }

    public int getSelectedCol() {
        return selectedCol;
    }

    // Move management
    public void addPendingMove(PendingMove move) {
        pendingMoves.add(move);
    }

    public List<PendingMove> getPendingMoves() {
        return new ArrayList<>(pendingMoves);
    }

    public boolean isPieceInFlight(int row, int col) {
        return pendingMoves.stream().anyMatch(m -> m.originatesFrom(row, col));
    }

    public boolean isTargetClaimed(int row, int col) {
        return pendingMoves.stream().anyMatch(m -> m.targetsCell(row, col));
    }

    public boolean hasActiveMoveOfOppositeColor(char currentColor) {
        char oppositeColor = (currentColor == Piece.WHITE) ? Piece.BLACK : Piece.WHITE;
        return pendingMoves.stream()
                .anyMatch(m -> m.getPiece().charAt(0) == oppositeColor);
    }

    // Jump management
    public void addJump(JumpState jump) {
        activeJumps.add(jump);
    }

    public List<JumpState> getActiveJumps() {
        return new ArrayList<>(activeJumps);
    }

    public boolean isPieceAirborne(int row, int col, long currentTime) {
        return activeJumps.stream()
                .anyMatch(j -> j.isAtLocation(row, col) && j.isActiveAt(currentTime));
    }

    public JumpState findActiveJumpAt(int row, int col, long currentTime) {
        return activeJumps.stream()
                .filter(j -> j.isAtLocation(row, col) && j.isActiveAt(currentTime))
                .findFirst()
                .orElse(null);
    }

    // Game status
    public void endGame() {
        this.gameOver = true;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    // Cleanup
    public void removePendingMove(PendingMove move) {
        pendingMoves.remove(move);
    }

    public void cleanupCompletedJumps(long currentTime) {
        activeJumps.removeIf(j -> j.hasLanded(currentTime));
    }

    public void reset() {
        clearSelection();
        gameOver = false;
        pendingMoves.clear();
        activeJumps.clear();
    }
}
