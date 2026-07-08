package model;

public class Board implements IBoard {
    private final String[][] grid;
    private final int numRows;
    private final int numCols;

    public Board(String[][] initialGrid) {
        this.numRows = initialGrid.length;
        this.numCols = initialGrid[0].length;
        this.grid = deepCopyGrid(initialGrid);
    }

    @Override
    public String getPieceAt(int row, int col) {
        validateCoordinates(row, col);
        return grid[row][col];
    }

    @Override
    public void setPieceAt(int row, int col, String piece) {
        validateCoordinates(row, col);
        grid[row][col] = piece;
    }

    @Override
    public void clearCell(int row, int col) {
        validateCoordinates(row, col);
        grid[row][col] = Piece.EMPTY;
    }

    @Override
    public int getNumRows() {
        return numRows;
    }

    @Override
    public int getNumCols() {
        return numCols;
    }

    @Override
    public boolean isWithinBounds(int row, int col) {
        return row >= 0 && row < numRows && col >= 0 && col < numCols;
    }

    @Override
    public void printBoard() {
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                System.out.print(grid[i][j]);
                if (j < numCols - 1) System.out.print(" ");
            }
            System.out.println();
        }
    }

    private void validateCoordinates(int row, int col) {
        if (!isWithinBounds(row, col)) {
            throw new IllegalArgumentException("Coordinates out of bounds: (" + row + ", " + col + ")");
        }
    }

    private String[][] deepCopyGrid(String[][] source) {
        String[][] copy = new String[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }
}