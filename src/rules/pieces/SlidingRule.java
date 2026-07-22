package src.rules.pieces;

import src.rules.PieceRules;
import src.model.IBoard;
import src.model.Piece;
import src.model.Position;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

abstract class SlidingRule implements PieceRules {

    static final int[][] STRAIGHT_DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };
    static final int[][] DIAGONAL_DIRECTIONS = {
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

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

            Optional<Piece> occupant = board.pieceAt(candidate);
            if (PieceRules.isCapturableOrEmpty(occupant, piece.color())) {
                destinations.add(candidate);
            }
            if (occupant.isPresent()) {
                return;
            }

            row += rowStep;
            col += colStep;
        }
    }
}
