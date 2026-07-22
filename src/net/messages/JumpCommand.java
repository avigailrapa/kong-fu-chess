package src.net.messages;

import src.model.Piece;
import src.model.Position;

public record JumpCommand(Piece.Color color, Piece.Kind kind, Position at) implements WireMessage {
}
