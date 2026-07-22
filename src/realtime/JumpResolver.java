package src.realtime;

import lombok.RequiredArgsConstructor;
import src.model.IBoard;
import src.model.Piece;
import src.model.Position;

import java.util.Optional;

@RequiredArgsConstructor
public class JumpResolver {

    private final IBoard board;

    public Optional<ArrivalEvent> resolveLanding(Piece jumper, Position cell) {
        Piece occupant = board.getPieceAt(cell).orElse(null);

        if (occupant == null || occupant == jumper) {
            markSurvivedJump(jumper);
            return Optional.empty();
        }

        boolean occupantWasKing = occupant.kind() == Piece.Kind.KING;
        occupant.setState(Piece.State.CAPTURED);
        board.removePiece(cell);
        board.addPiece(jumper, cell);
        markSurvivedJump(jumper);
        return Optional.of(new ArrivalEvent(jumper, cell, cell, occupant, occupantWasKing, false));
    }

    public void markSurvivedJump(Piece jumper) {
        jumper.setState(Piece.State.IDLE);
    }
}
