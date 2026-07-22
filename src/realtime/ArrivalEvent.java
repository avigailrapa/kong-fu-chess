package src.realtime;

import src.model.Piece;
import src.model.Position;

public record ArrivalEvent(Piece movedPiece, Position from, Position to, Piece capturedPiece, boolean kingCaptured,
                            boolean promoted) {
}
