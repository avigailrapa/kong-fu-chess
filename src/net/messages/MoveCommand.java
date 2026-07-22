package src.net.messages;

import src.model.Piece;
import src.model.Position;

public record MoveCommand(Piece.Color color, Piece.Kind kind, Position from, Position to) implements WireMessage {
}
