package model;

public class Piece {
    public static final char WHITE = 'w';
    public static final char BLACK = 'b';
    public static final char KING = 'K';
    public static final char QUEEN = 'Q';
    public static final char ROOK = 'R';
    public static final char BISHOP = 'B';
    public static final char KNIGHT = 'N';
    public static final char PAWN = 'P';
    public static final String EMPTY = ".";

    private final char color;
    private final char type;

    private Piece(char color, char type) {
        this.color = color;
        this.type = type;
    }

    public static Piece create(char color, char type) {
        if (!isValidColor(color) || !isValidType(type)) {
            throw new IllegalArgumentException("Invalid piece: color=" + color + ", type=" + type);
        }
        return new Piece(color, type);
    }

    public static Piece fromString(String piece) {
        if (piece == null || piece.isEmpty()) {
            throw new IllegalArgumentException("features.Piece string cannot be null or empty");
        }
        if (piece.equals(EMPTY)) {
            throw new IllegalArgumentException("Use isEmpty() for empty cells");
        }
        if (piece.length() != 2) {
            throw new IllegalArgumentException("features.Piece string must be 2 characters: " + piece);
        }
        return new Piece(piece.charAt(0), piece.charAt(1));
    }

    public static boolean isValidToken(String token) {
        if (token.equals(EMPTY)) return true;
        if (token.length() != 2) return false;
        return isValidColor(token.charAt(0)) && isValidType(token.charAt(1));
    }

    public char getColor() {
        return color;
    }

    public char getType() {
        return type;
    }

    public String toString() {
        return String.valueOf(color) + type;
    }

    public boolean isWhite() {
        return color == WHITE;
    }

    public boolean isBlack() {
        return color == BLACK;
    }

    public boolean isSameColorAs(Piece other) {
        return this.color == other.color;
    }

    public boolean isKing() {
        return type == KING;
    }

    private static boolean isValidColor(char color) {
        return color == WHITE || color == BLACK;
    }

    private static boolean isValidType(char type) {
        return type == 'K' || type == 'Q' || type == 'R' || type == 'B' || type == 'N' || type == 'P';
    }
}
