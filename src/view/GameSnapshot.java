package src.view;

import src.model.Piece;
import src.model.Position;

import java.util.List;
import java.util.Set;

public record GameSnapshot(int width, int height, PieceSnapshot[][] board, List<SelectionSnapshot> selections,
                            Set<Position> legalDestinations,
                            boolean gameOver, Piece.Color winner, int whiteScore, int blackScore,
                            List<String> whiteMoveLog, List<String> blackMoveLog) {

    private static final double BOARD_SCALE = 0.9;
    public static final double CELL_WIDTH = 822.0 * BOARD_SCALE / 8;
    public static final double CELL_HEIGHT = 828.0 * BOARD_SCALE / 8;

    public PieceSnapshot pieceAt(Position position) {
        return board[position.row()][position.col()];
    }

    public boolean isOccupied(Position position) {
        return pieceAt(position) != null;
    }
}
