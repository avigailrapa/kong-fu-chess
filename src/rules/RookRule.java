package rules;

import model.IBoard;
import model.Piece;
import model.Position;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class RookRule implements PieceRules {

    private static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };

    @Override
    public Set<Position> legalDestinations(IBoard board, Piece piece) {
        Set<Position> destinations = new HashSet<>();

        for (int[] direction : DIRECTIONS) {
            slide(board, piece, direction[0], direction[1], destinations);
        }

        return destinations;
    }

    private void slide(IBoard board, Piece piece, int rowStep, int colStep, Set<Position> destinations) {
        Position current = piece.getCell();
        int row = current.getRow() + rowStep;
        int col = current.getCol() + colStep;

        while (true) {
            Position candidate = new Position(row, col);
            if (!board.isWithinBorder(candidate)) {
                return;
            }

            Optional<Piece> occupant = board.getPieceAt(candidate);
            if (occupant.isEmpty()) {
                destinations.add(candidate);
            } else {
                if (occupant.get().getColor() != piece.getColor()) {
                    destinations.add(candidate);
                }
                return;
            }

            row += rowStep;
            col += colStep;
        }
    }
}
