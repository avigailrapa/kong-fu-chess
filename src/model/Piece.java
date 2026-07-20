package src.model;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Objects;

@Accessors(fluent = true)
public class Piece {

    public enum Color {
        WHITE, BLACK;

        public char letter() {
            return name().charAt(0);
        }

        public static Color fromLetter(char letter) {
            return switch (Character.toUpperCase(letter)) {
                case 'W' -> WHITE;
                case 'B' -> BLACK;
                default -> throw new IllegalArgumentException("Invalid piece color letter: " + letter);
            };
        }
    }

    public enum Kind {
        KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN;

        public char letter() {
            return switch (this) {
                case KING -> 'K';
                case QUEEN -> 'Q';
                case ROOK -> 'R';
                case BISHOP -> 'B';
                case KNIGHT -> 'N';
                case PAWN -> 'P';
            };
        }

        public static Kind fromLetter(char letter) {
            return switch (Character.toUpperCase(letter)) {
                case 'K' -> KING;
                case 'Q' -> QUEEN;
                case 'R' -> ROOK;
                case 'B' -> BISHOP;
                case 'N' -> KNIGHT;
                case 'P' -> PAWN;
                default -> throw new IllegalArgumentException("Invalid piece kind letter: " + letter);
            };
        }
    }

    public enum State {
        IDLE, MOVING, JUMPING, SHORT_REST, LONG_REST, CAPTURED
    }

    @Getter
    private final String id;
    @Getter
    private final Color color;
    @Getter
    private final Kind kind;
    @Getter
    private Position cell;
    @Getter
    private State state;

    public Piece(String id, Color color, Kind kind, Position cell) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.color = Objects.requireNonNull(color, "color must not be null");
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.cell = Objects.requireNonNull(cell, "cell must not be null");
        this.state = State.IDLE;
    }

    public void setCell(Position cell) {
        this.cell = Objects.requireNonNull(cell, "cell must not be null");
    }

    public void setState(State state) {
        this.state = Objects.requireNonNull(state, "state must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Piece other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return color + " " + kind + " #" + id + " @" + cell + " [" + state + "]";
    }
}
