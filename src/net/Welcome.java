package src.net;

import src.model.Piece;

public record Welcome(Piece.Color color, int rating) implements WireMessage {
}
