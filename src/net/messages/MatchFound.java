package src.net.messages;

import src.model.Piece;

public record MatchFound(String opponentUsername, Piece.Color assignedColor, int opponentRating) implements WireMessage {
}
