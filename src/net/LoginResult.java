package src.net;

import src.model.Piece;

public record LoginResult(boolean accepted, Piece.Color assignedColor, int rating, String reason) {
}
