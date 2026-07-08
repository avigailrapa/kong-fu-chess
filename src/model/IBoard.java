package model;

public interface IBoard {
    String getPieceAt(int row, int col);
    void setPieceAt(int row, int col, String piece);
    void clearCell(int row, int col);
    int getNumRows();
    int getNumCols();
    boolean isWithinBounds(int row, int col);
    void printBoard();
}
