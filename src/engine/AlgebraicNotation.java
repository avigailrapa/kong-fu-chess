package src.engine;

import src.model.Position;

public final class AlgebraicNotation {

    private AlgebraicNotation() {
    }

    public static String toSquare(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("position must not be null");
        }
        if (position.row() < 0 || position.row() > 7 || position.col() < 0 || position.col() > 7) {
            throw new IllegalArgumentException("position must be within the 8x8 board: " + position);
        }
        char file = (char) ('a' + position.col());
        int rank = 8 - position.row();
        return "" + file + rank;
    }

    public static Position toPosition(String square) {
        if (square == null || square.length() != 2) {
            throw new IllegalArgumentException("square must be exactly 2 characters, e.g. \"e7\": " + square);
        }
        char file = square.charAt(0);
        char rank = square.charAt(1);
        if (file < 'a' || file > 'h') {
            throw new IllegalArgumentException("file must be between 'a' and 'h': " + square);
        }
        if (rank < '1' || rank > '8') {
            throw new IllegalArgumentException("rank must be between '1' and '8': " + square);
        }
        int col = file - 'a';
        int row = 8 - (rank - '0');
        return new Position(row, col);
    }
}
