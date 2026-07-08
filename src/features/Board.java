package features;

public class Board {
    private final String[][] grid;
    private final int numRows;
    private final int numCols;

    public Board(String[][] initialGrid) {
        this.numRows = initialGrid.length;
        this.numCols = initialGrid[0].length;
        this.grid = deepCopyGrid(initialGrid);
    }

    public String getPieceAt(int row, int col) {
        validateCoordinates(row, col);
        return grid[row][col];
    }

    public void setPieceAt(int row, int col, String piece) {
        validateCoordinates(row, col);
        grid[row][col] = piece;
    }

    public void clearCell(int row, int col) {
        validateCoordinates(row, col);
        grid[row][col] = ".";
    }

    public int getNumRows() {
        return numRows;
    }

    public int getNumCols() {
        return numCols;
    }

    public boolean isWithinBounds(int row, int col) {
        return row >= 0 && row < numRows && col >= 0 && col < numCols;
    }

    public void printBoard() {
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                System.out.print(grid[i][j]);
                if (j < numCols - 1) System.out.print(" ");
            }
            System.out.println();
        }
    }

    public String[][] getGrid() {
        return deepCopyGrid(grid);
    }

    private void validateCoordinates(int row, int col) {
        if (!isWithinBounds(row, col)) {
            throw new IllegalArgumentException("Coordinates out of bounds: (" + row + ", " + col + ")");
        }
    }

    private String[][] deepCopyGrid(String[][] source) {
        String[][] copy = new String[source.length][source[0].length];
        for (int i = 0; i < source.length; i++) {
            System.arraycopy(source[i], 0, copy[i], 0, source[i].length);
        }
        return copy;
    }
}
