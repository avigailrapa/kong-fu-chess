package src.realtime;

import src.model.Board;
import src.model.Piece;
import src.model.Position;

import java.util.Optional;

public class JumpResolver {

    private final Board board;

    public JumpResolver(Board board) {
        this.board = board;
    }

    public Optional<ArrivalEvent> resolveLanding(Piece defender, Position cell) {
        Piece occupant = board.getPieceAt(cell).orElse(null);

        if (occupant == defender) {
            markSurvivedJump(defender);
            return Optional.empty();
        }

        defender.setState(Piece.State.CAPTURED);
        boolean defenderWasKing = defender.getKind() == Piece.Kind.KING;
        return Optional.of(new ArrivalEvent(defender, cell, cell, occupant, defenderWasKing));
    }

    public void markSurvivedJump(Piece defender) {
        defender.setState(Piece.State.IDLE);
    }
}
