package rules;

import model.IBoard;
import model.Piece;
import model.Position;

import java.util.Set;

public interface PieceRules {
    Set<Position> legalDestinations(IBoard board, Piece piece);
}
