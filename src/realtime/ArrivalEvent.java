package realtime;

import model.Piece;
import model.Position;

public record ArrivalEvent(Piece movedPiece, Position from, Position to, Piece capturedPiece, boolean kingCaptured) {
}
