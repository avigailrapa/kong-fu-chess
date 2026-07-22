package src.rules;

import src.model.IBoard;
import src.model.Piece;
import src.model.Position;

import java.util.Optional;
import java.util.Set;

public interface PieceRules {
    Set<Position> legalDestinations(IBoard board, Piece piece);

    static boolean isCapturableOrEmpty(Optional<Piece> occupant, Piece.Color mover) {
        return occupant.isEmpty() || occupant.get().color() != mover;
    }
}
