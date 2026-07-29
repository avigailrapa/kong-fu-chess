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
import src.server.core.SessionRegistry;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SessionRegistryConcurrencyTest {

    private Match dummyMatch() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0)), new Position(7, 0));
        Map<Piece.Kind, PieceRules> rulesByKind = Map.of(Piece.Kind.ROOK, new RookRule());
        RuleEngine ruleEngine = new RuleEngine(rulesByKind);
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine, new RealTimeArbiter(board));
        return new Match(engine, 1000);
    }

    @Test
    public void testConcurrentRegisterBindAndUnregisterFromManyThreadsDoesNotCorruptState() throws Exception {
        SessionRegistry registry = new SessionRegistry();
        Match sharedMatch = dummyMatch();
        int threadCount = 16;
        int perThread = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        try {
            var futures = IntStream.range(0, threadCount)
                    .mapToObj(threadIndex -> pool.submit(() -> {
                        for (int i = 0; i < perThread; i++) {
                            Object conn = new Object();
                            Session session = new Session(null, "user-" + threadIndex + "-" + i, 1200);

                            registry.register(conn, session);
                            registry.bind(session, sharedMatch);

                            assertSame(session, registry.sessionFor(conn));
                            assertSame(sharedMatch, registry.matchFor(conn));
                            assertSame(sharedMatch, registry.matchFor(session));
                            assertTrue(registry.isBound(session));

                            registry.unregister(conn);
                            assertNull(registry.sessionFor(conn));
                        }
                    }))
                    .collect(Collectors.toList());

            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdown();
        }
    }
}
