package rules;

import model.IBoard;
import model.Piece;
import model.Position;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

abstract class FixedOffsetRule implements PieceRules {

    protected abstract int[][] offsets();

    @Override
    public Set<Position> legalDestinations(IBoard board, Piece piece) {
        Set<Position> destinations = new HashSet<>();
        Position current = piece.getCell();

        for (int[] offset : offsets()) {
            Position candidate = new Position(current.getRow() + offset[0], current.getCol() + offset[1]);
            if (!board.isWithinBorder(candidate)) {
                continue;
            }

            Optional<Piece> occupant = board.getPieceAt(candidate);
            if (occupant.isEmpty() || occupant.get().getColor() != piece.getColor()) {
                destinations.add(candidate);
            }
        }

        return destinations;
    }
}
