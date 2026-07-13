package src.realtime;

import src.model.Board;
import src.model.Piece;
import src.model.Position;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CollisionResolver {

    private final Board board;
    private final JumpResolver jumpResolver;

    public CollisionResolver(Board board, JumpResolver jumpResolver) {
        this.board = board;
        this.jumpResolver = jumpResolver;
    }

    public Optional<Piece> findMotionLandingOnJumpCell(Position jumpCell, List<Piece> duePieces, Map<Piece, Motion> activeMotions) {
        for (Piece candidate : duePieces) {
            Motion motion = activeMotions.get(candidate);
            if (motion != null && motion.destination().equals(jumpCell)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    public ArrivalEvent resolveJumpCountersAttackingMotion(Piece defender, Position cell, Motion attackerMotion) {
        Piece attacker = attackerMotion.piece();
        boolean attackerWasKing = attacker.getKind() == Piece.Kind.KING;
        attacker.setState(Piece.State.CAPTURED);
        board.removePiece(attackerMotion.source());

        jumpResolver.markSurvivedJump(defender);
        return new ArrivalEvent(defender, cell, cell, attacker, attackerWasKing);
    }

    public boolean isPieceAlreadyCaptured(Motion motion) {
        return motion.piece().getState() == Piece.State.CAPTURED;
    }

    public boolean isMotionLandingOnAirbornePiece(Motion motion, Set<Piece> airbornePieces) {
        if (airbornePieces.isEmpty()) {
            return false;
        }
        Piece occupant = board.getPieceAt(motion.destination()).orElse(null);
        return occupant != null && airbornePieces.contains(occupant);
    }

    public void clearCellForDisplacement(Position cell) {
        board.removePiece(cell);
    }
}
