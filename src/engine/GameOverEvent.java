package src.engine;

import src.model.Piece;

public record GameOverEvent(Piece.Color winner) {
}
