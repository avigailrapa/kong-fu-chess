package src.realtime;

import src.model.Piece;
import src.model.Position;

public class Motion {
    private final Piece piece;
    private final Position source;
    private final Position destination;
    private final long durationMs;

    public Motion(Piece piece, Position source, Position destination, long durationMs) {
        this.piece = piece;
        this.source = source;
        this.destination = destination;
        this.durationMs = durationMs;
    }

    public Piece piece() {
        return piece;
    }

    public Position source() {
        return source;
    }

    public Position destination() {
        return destination;
    }

    public long durationMs() {
        return durationMs;
    }
}