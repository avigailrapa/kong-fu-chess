package src.realtime;

import src.model.Piece;
import src.model.Position;

public record Motion(Piece piece, Position source, Position destination, long durationMs) {
}
