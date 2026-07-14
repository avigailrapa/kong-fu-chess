package src.model;

import java.util.Objects;

public class Piece {

    public enum Color {
        WHITE, BLACK
    }

    public enum Kind {
        KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
    }

    public enum State {
        IDLE, MOVING, JUMPING, LONG_REST, SHORT_REST, CAPTURED
    }

    private final String id;
    private final Color color;
    private final Kind kind;
    private Position cell;
    private State state;

    public Piece(String id, Color color, Kind kind, Position cell) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.color = Objects.requireNonNull(color, "color must not be null");
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.cell = Objects.requireNonNull(cell, "cell must not be null");
        this.state = State.IDLE;
    }

    public String getId() {
        return id;
    }

    public Color getColor() {
        return color;
    }

    public Kind getKind() {
        return kind;
    }

    public Position getCell() {
        return cell;
    }

    public State getState() {
        return state;
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
        if (!(o instanceof Piece)) return false;
        return id.equals(((Piece) o).id);
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
