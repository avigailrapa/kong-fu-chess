package src.rules.pieces;

import src.rules.PieceRules;
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
        Position current = piece.cell();
        int forward = piece.color() == Piece.Color.WHITE ? -1 : 1;
        int startingRow = piece.color() == Piece.Color.WHITE ? board.height() - 2 : 1;

        Position forwardCell = new Position(current.row() + forward, current.col());
        boolean forwardOpen = board.isWithinBorder(forwardCell) && board.pieceAt(forwardCell).isEmpty();
        if (forwardOpen) {
            destinations.add(forwardCell);

            if (current.row() == startingRow) {
                Position twoStepCell = new Position(current.row() + forward * 2, current.col());
                if (board.isWithinBorder(twoStepCell) && board.pieceAt(twoStepCell).isEmpty()) {
                    destinations.add(twoStepCell);
                }
            }
        }

        for (int colOffset : new int[]{-1, 1}) {
            Position diagonalCell = new Position(current.row() + forward, current.col() + colOffset);
            if (!board.isWithinBorder(diagonalCell)) {
                continue;
            }
            Optional<Piece> occupant = board.pieceAt(diagonalCell);
            if (occupant.isPresent() && occupant.get().color() != piece.color()) {
                destinations.add(diagonalCell);
            }
        }

        return destinations;
    }
}
