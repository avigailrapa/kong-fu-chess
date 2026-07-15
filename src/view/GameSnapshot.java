package src.view;

import src.model.Piece;
import src.model.Position;

import java.util.Set;

public record GameSnapshot(int width, int height, PieceSnapshot[][] board, Position selectedPosition,
                            Set<Position> legalDestinations,
                            boolean gameOver, Piece.Color winner, int whiteScore, int blackScore) {

    public static final double CELL_WIDTH = 822.0 / 8;
    public static final double CELL_HEIGHT = 828.0 / 8;

    public PieceSnapshot pieceAt(Position position) {
        return board[position.getRow()][position.getCol()];
    }

    public boolean isOccupied(Position position) {
        return pieceAt(position) != null;
    }

    public boolean isLegalDestination(Position position) {
        return legalDestinations.contains(position);
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public int getWhiteScore() {
        return whiteScore;
    }

    public int getBlackScore() {
        return blackScore;
    }
}
