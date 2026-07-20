package src.rules;

import src.model.IBoard;
import src.model.Piece;
import src.model.Position;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

abstract class SlidingRule implements PieceRules {

    protected abstract int[][] directions();

    @Override
    public Set<Position> legalDestinations(IBoard board, Piece piece) {
        Set<Position> destinations = new HashSet<>();

        for (int[] direction : directions()) {
            slide(board, piece, direction[0], direction[1], destinations);
        }

        return destinations;
    }

    private void slide(IBoard board, Piece piece, int rowStep, int colStep, Set<Position> destinations) {
        Position current = piece.cell();
        int row = current.row() + rowStep;
        int col = current.col() + colStep;

        while (true) {
            Position candidate = new Position(row, col);
            if (!board.isWithinBorder(candidate)) {
                return;
            }

            Optional<Piece> occupant = board.getPieceAt(candidate);
            if (occupant.isEmpty()) {
                destinations.add(candidate);
            } else {
                if (occupant.get().color() != piece.color()) {
                    destinations.add(candidate);
                }
                return;
            }

            row += rowStep;
            col += colStep;
        }
    }
}
