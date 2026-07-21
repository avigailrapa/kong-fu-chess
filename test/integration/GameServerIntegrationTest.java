package integration;

import org.junit.jupiter.api.Test;
import src.engine.GameEngine;
import src.model.Board;
import src.model.GameState;
import src.model.Piece;
import src.model.Position;
import src.realtime.RealTimeArbiter;
import src.rules.PieceRules;
import src.rules.RookRule;
import src.rules.RuleEngine;
import src.server.GameServer;
import src.server.Match;
import src.server.UserStore;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class GameServerIntegrationTest {

    @Test
    public void testRealSocketRoundTripAcceptsAMoveAndBroadcastsState() throws Exception {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0)), new Position(7, 0));
        Map<Piece.Kind, PieceRules> rulesByKind = Map.of(Piece.Kind.ROOK, new RookRule());
        GameEngine engine = new GameEngine(board, new GameState(), new RuleEngine(rulesByKind), new RealTimeArbiter(board));
        Match match = new Match(engine, 100);
        GameServer server = new GameServer(new InetSocketAddress("localhost", 0), match,
                new UserStore("jdbc:sqlite::memory:"));
        server.start();

        AtomicInteger stateCount = new AtomicInteger();
        CountDownLatch gotWelcome = new CountDownLatch(1);
        CountDownLatch gotOk = new CountDownLatch(1);
        CountDownLatch gotStateAfterLogin = new CountDownLatch(1);
        WebSocket.Listener listener = new WebSocket.Listener() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                buffer.append(data);
                if (last) {
                    String message = buffer.toString();
                    buffer.setLength(0);
                    if (message.equals("OK")) {
                        gotOk.countDown();
                    } else if (message.equals("WELCOME W 1200")) {
                        gotWelcome.countDown();
                    } else if (message.startsWith("STATE ")) {
                        stateCount.incrementAndGet();
                        gotStateAfterLogin.countDown();
                    }
                }
                webSocket.request(1);
                return null;
            }
        };

        WebSocket client = null;
        try {
            int port = waitForBoundPort(server);
            client = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:" + port), listener)
                    .get(5, TimeUnit.SECONDS);

            // The server no longer pushes STATE on raw connect - a client must LOGIN first (Level 2).
            // LOGIN's own reply-then-broadcast still produces one STATE, which this captures before
            // triggering a move, so the later assertion proves the move caused an *additional* broadcast.
            client.sendText("LOGIN alice pw", true).get(5, TimeUnit.SECONDS);
            assertTrue(gotWelcome.await(5, TimeUnit.SECONDS), "expected WELCOME W over the real socket");
            assertTrue(gotStateAfterLogin.await(5, TimeUnit.SECONDS), "expected a STATE broadcast after login");
            int stateCountBeforeMove = stateCount.get();

            client.sendText("WRa1a4", true).get(5, TimeUnit.SECONDS);

            assertTrue(gotOk.await(5, TimeUnit.SECONDS), "expected an OK reply over the real socket");
            long deadline = System.currentTimeMillis() + 5000;
            while (stateCount.get() <= stateCountBeforeMove && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertTrue(stateCount.get() > stateCountBeforeMove,
                    "expected a STATE broadcast triggered by the move, not just the initial connect");
        } finally {
            if (client != null) {
                client.abort();
            }
            match.stop();
            server.stop();
        }
    }

    private int waitForBoundPort(GameServer server) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            int port = server.getPort();
            if (port > 0) {
                return port;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("server did not bind a port in time");
    }
}
