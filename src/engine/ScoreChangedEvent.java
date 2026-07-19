package src.engine;

import src.model.Piece;

public record ScoreChangedEvent(Piece.Color color, int newScore, int delta) {
}
