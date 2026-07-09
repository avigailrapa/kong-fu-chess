package rules;

import model.IBoard;
import model.Piece;
import model.Position;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class PawnRule implements PieceRules {

    @Override
    public Set<Position> legalDestinations(IBoard board, Piece piece) {
        Set<Position> destinations = new HashSet<>();
        Position current = piece.getCell();
        int forward = piece.getColor() == Piece.Color.WHITE ? -1 : 1;

        Position forwardCell = new Position(current.getRow() + forward, current.getCol());
        if (board.isWithinBorder(forwardCell) && board.getPieceAt(forwardCell).isEmpty()) {
            destinations.add(forwardCell);
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
