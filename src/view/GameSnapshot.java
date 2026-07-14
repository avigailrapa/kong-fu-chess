package src.view;

import src.model.Piece;
import src.model.Position;

public class GameSnapshot {
    // Measured from CTD26/board.png (822x828px) divided across an 8x8 board - the image has
    // no border, so a cell's real pixel size is not the round number it would be if CELL_SIZE
    // were square. Using the wrong value here made piece placement drift further off the grid
    // with every row/column.
    public static final double CELL_WIDTH = 822.0 / 8;
    public static final double CELL_HEIGHT = 828.0 / 8;

    private final int width;
    private final int height;
    private final PieceSnapshot[][] board;
    private final Position selectedPosition;
    private final boolean gameOver;
    private final Piece.Color winner;

    public GameSnapshot(int width, int height, PieceSnapshot[][] board, Position selectedPosition,
                         boolean gameOver, Piece.Color winner) {
        this.width = width;
        this.height = height;
        this.board = board;
        this.selectedPosition = selectedPosition;
        this.gameOver = gameOver;
        this.winner = winner;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public PieceSnapshot pieceAt(Position position) {
        return board[position.getRow()][position.getCol()];
    }

    public boolean isOccupied(Position position) {
        return pieceAt(position) != null;
    }

    public Position selectedPosition() {
        return selectedPosition;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Piece.Color winner() {
        return winner;
    }
}
