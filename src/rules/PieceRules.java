package src.rules;

import src.model.IBoard;
import src.model.Piece;
import src.model.Position;

import java.util.Set;

public interface PieceRules {
    Set<Position> legalDestinations(IBoard board, Piece piece);
}
