package src.rules;

import src.model.IBoard;
import src.model.Piece;
import src.model.Position;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class PawnRule implements PieceRules {

    @Override
    public Set<Position> legalDestinations(IBoard board, Piece piece) {
        Set<Position> destinations = new HashSet<>();
        Position current = piece.getCell();
        int forward = piece.getColor() == Piece.Color.WHITE ? -1 : 1;
        int startingRow = piece.getColor() == Piece.Color.WHITE ? board.getHeight() - 2 : 1;

        Position forwardCell = new Position(current.getRow() + forward, current.getCol());
        boolean forwardOpen = board.isWithinBorder(forwardCell) && board.getPieceAt(forwardCell).isEmpty();
        if (forwardOpen) {
            destinations.add(forwardCell);

            if (current.getRow() == startingRow) {
                Position twoStepCell = new Position(current.getRow() + forward * 2, current.getCol());
                if (board.isWithinBorder(twoStepCell) && board.getPieceAt(twoStepCell).isEmpty()) {
                    destinations.add(twoStepCell);
                }
            }
        }

        for (int colOffset : new int[]{-1, 1}) {
            Position diagonalCell = new Position(current.getRow() + forward, current.getCol() + colOffset);
            if (!board.isWithinBorder(diagonalCell)) {
                continue;
            }
            Optional<Piece> occupant = board.getPieceAt(diagonalCell);
            if (occupant.isPresent() && occupant.get().getColor() != piece.getColor()) {
                destinations.add(diagonalCell);
            }
        }

        return destinations;
    }
}
