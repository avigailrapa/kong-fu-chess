package src.view;

import src.model.Piece;
import src.model.Position;

import java.util.List;
import java.util.Set;

public record GameSnapshot(int width, int height, PieceSnapshot[][] board, List<SelectionSnapshot> selections,
                            Set<Position> legalDestinations,
                            boolean gameOver, Piece.Color winner, int whiteScore, int blackScore,
                            List<String> whiteMoveLog, List<String> blackMoveLog, double zoom) {

    private static final double BOARD_SCALE = 0.9;
    public static final double CELL_WIDTH = 822.0 * BOARD_SCALE / 8;
    public static final double CELL_HEIGHT = 828.0 * BOARD_SCALE / 8;

    public static final double MIN_ZOOM = 0.5;
    public static final double MAX_ZOOM = 2.0;
    public static final double DEFAULT_ZOOM = 1.0;

    public double cellWidth() {
        return CELL_WIDTH * zoom;
    }

    public double cellHeight() {
        return CELL_HEIGHT * zoom;
    }

    public PieceSnapshot pieceAt(Position position) {
        return board[position.row()][position.col()];
    }

    public boolean isOccupied(Position position) {
        return pieceAt(position) != null;
    }
}
