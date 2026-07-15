package src.view;

import src.model.Piece;
import src.model.Position;

public record SelectionSnapshot(Piece.Color playerId, Position position) {
}
