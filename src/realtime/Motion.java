package realtime;

import model.Piece;
import model.Position;

public record Motion(Piece piece, Position source, Position destination, long durationMs) {
}
