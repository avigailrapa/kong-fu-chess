package src.engine;

import src.model.Position;

public final class AlgebraicNotation {

    private static final int BOARD_DIMENSION = 8;

    private AlgebraicNotation() {
    }

    public static String toSquare(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("position must not be null");
        }
        if (position.row() < 0 || position.row() >= BOARD_DIMENSION
                || position.col() < 0 || position.col() >= BOARD_DIMENSION) {
            throw new IllegalArgumentException("position must be within the 8x8 board: " + position);
        }
        char file = (char) ('a' + position.col());
        int rank = BOARD_DIMENSION - position.row();
        return "" + file + rank;
    }

    public static Position toPosition(String square) {
        if (square == null || square.length() != 2) {
            throw new IllegalArgumentException("square must be exactly 2 characters, e.g. \"e7\": " + square);
        }
        char file = square.charAt(0);
        char rank = square.charAt(1);
        if (file < 'a' || file >= 'a' + BOARD_DIMENSION) {
            throw new IllegalArgumentException("file must be between 'a' and 'h': " + square);
        }
        if (rank < '1' || rank >= '1' + BOARD_DIMENSION) {
            throw new IllegalArgumentException("rank must be between '1' and '8': " + square);
        }
        int col = file - 'a';
        int row = BOARD_DIMENSION - (rank - '0');
        return new Position(row, col);
    }
}
