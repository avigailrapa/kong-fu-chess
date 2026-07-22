package src.rules;

import src.model.IBoard;
import src.model.Piece;
import src.model.Position;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

abstract class FixedOffsetRule implements PieceRules {

    protected abstract int[][] offsets();

    @Override
    public Set<Position> legalDestinations(IBoard board, Piece piece) {
        Set<Position> destinations = new HashSet<>();
        Position current = piece.cell();

        for (int[] offset : offsets()) {
            Position candidate = new Position(current.row() + offset[0], current.col() + offset[1]);
            if (!board.isWithinBorder(candidate)) {
                continue;
            }

            Optional<Piece> occupant = board.pieceAt(candidate);
            if (occupant.isEmpty() || occupant.get().color() != piece.color()) {
                destinations.add(candidate);
            }
        }

        return destinations;
    }
}
