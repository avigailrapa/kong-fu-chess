package src.view;

import src.model.Piece;

public class PieceSnapshot {
    private final String id;
    private final Piece.Color color;
    private final Piece.Kind kind;
    private final Piece.State state;
    private final int pixelX;
    private final int pixelY;
    private final long elapsedMillis;

    public PieceSnapshot(String id, Piece.Color color, Piece.Kind kind, Piece.State state,
                          int pixelX, int pixelY, long elapsedMillis) {
        this.id = id;
        this.color = color;
        this.kind = kind;
        this.state = state;
        this.pixelX = pixelX;
        this.pixelY = pixelY;
        this.elapsedMillis = elapsedMillis;
    }

    public String id() {
        return id;
    }

    public Piece.Color color() {
        return color;
    }

    public Piece.Kind kind() {
        return kind;
    }

    public Piece.State state() {
        return state;
    }

    public int pixelX() {
        return pixelX;
    }

    public int pixelY() {
        return pixelY;
    }

    public long elapsedMillis() {
        return elapsedMillis;
    }
}
