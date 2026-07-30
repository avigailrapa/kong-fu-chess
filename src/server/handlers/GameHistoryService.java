package src.server.handlers;

import src.engine.GameOverEvent;
import src.engine.MoveEvent;
import src.model.Piece;
import src.server.core.Match;
import src.server.core.Session;
import src.server.history.GameStore;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GameHistoryService {

    private static final String MOVE_DELIMITER = ";";

    private final GameStore gameStore;

    public GameHistoryService(GameStore gameStore) {
        this.gameStore = gameStore;
    }

    public void recordGameOver(Match match, GameOverEvent event) {
        Session white = match.seatedWhite();
        Session black = match.seatedBlack();
        if (white == null || black == null) {
            return;
        }
        String winner = event.winner() == null ? "-" : String.valueOf(event.winner().letter());
        String whiteMoves = joinMoves(match.moveLogger().whiteMoves());
        String blackMoves = joinMoves(match.moveLogger().blackMoves());
        gameStore.save(UUID.randomUUID().toString(), white.username(), black.username(), winner, whiteMoves,
                blackMoves);
    }

    private String joinMoves(List<MoveEvent> moves) {
        return moves.stream().map(MoveEvent::algebraicMove).collect(Collectors.joining(MOVE_DELIMITER));
    }
}
