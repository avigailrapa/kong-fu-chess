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
    private final MotionResolver motionResolver;

    public CollisionResolver(Board board, JumpResolver jumpResolver, MotionResolver motionResolver) {
        this.board = board;
        this.jumpResolver = jumpResolver;
        this.motionResolver = motionResolver;
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

    // Smallest overshoot (elapsedMs - durationMs) wins: whoever crossed its own finish line most
    // recently arrived later and captures. Ties keep the first competitor (motion-start order).
    public Piece pickRaceWinner(List<Piece> competitors, Map<Piece, Motion> activeMotions, Map<Piece, Long> motionElapsedMs) {
        Piece winner = null;
        long bestOvershoot = Long.MAX_VALUE;
        for (Piece candidate : competitors) {
            long overshoot = motionElapsedMs.get(candidate) - activeMotions.get(candidate).durationMs();
            if (overshoot < bestOvershoot) {
                bestOvershoot = overshoot;
                winner = candidate;
            }
        }
        return winner;
    }

    public ArrivalEvent resolveRaceLoserAgainstWinner(Piece winner, Motion winnerMotion, Motion loserMotion) {
        Piece loser = loserMotion.piece();
        if (loser.getColor() != winner.getColor()) {
            boolean loserWasKing = loser.getKind() == Piece.Kind.KING;
            loser.setState(Piece.State.CAPTURED);
            board.removePiece(loserMotion.source());
            return new ArrivalEvent(winner, winnerMotion.source(), winnerMotion.destination(), loser, loserWasKing);
        }
        return motionResolver.resolveBounceBack(loserMotion);
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
