package server;

import org.junit.jupiter.api.Test;
import src.engine.GameEngine;
import src.model.Board;
import src.model.GameState;
import src.model.Piece;
import src.model.Position;
import src.realtime.RealTimeArbiter;
import src.rules.PieceRules;
import src.rules.RuleEngine;
import src.rules.pieces.RookRule;
import src.server.core.Match;
import src.server.core.Session;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MatchConcurrencyTest {

    private Match freshMatch() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0)), new Position(7, 0));
        Map<Piece.Kind, PieceRules> rulesByKind = Map.of(Piece.Kind.ROOK, new RookRule());
        RuleEngine ruleEngine = new RuleEngine(rulesByKind);
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine, new RealTimeArbiter(board));
        return new Match(engine, 1000);
    }

    @Test
    public void testConcurrentSpectatorAdditionsSurviveConcurrentReads() throws Exception {
        Match match = freshMatch();
        int spectatorCount = 200;
        List<Session> spectators = IntStream.range(0, spectatorCount)
                .mapToObj(i -> new Session(null, "spectator-" + i, 1200))
                .collect(Collectors.toList());

        ExecutorService writers = Executors.newFixedThreadPool(8);
        AtomicBoolean keepReading = new AtomicBoolean(true);
        ExecutorService readers = Executors.newFixedThreadPool(4);

        try {
            List<Future<?>> readerFutures = IntStream.range(0, 4)
                    .mapToObj(i -> readers.submit(() -> {
                        while (keepReading.get()) {
                            match.spectators().size();
                            match.seated().size();
                        }
                    }))
                    .collect(Collectors.toList());

            List<Future<?>> writerFutures = spectators.stream()
                    .map(session -> writers.submit(() -> match.addSpectator(session)))
                    .collect(Collectors.toList());

            for (Future<?> future : writerFutures) {
                future.get(10, TimeUnit.SECONDS);
            }
            keepReading.set(false);
            for (Future<?> future : readerFutures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            writers.shutdown();
            readers.shutdown();
        }

        assertEquals(spectatorCount, match.spectators().size());
        assertEquals(Set.copyOf(spectators), Set.copyOf(match.spectators()));
    }
}
