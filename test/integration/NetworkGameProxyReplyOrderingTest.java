package integration;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.junit.jupiter.api.Test;
import src.engine.MoveResult;
import src.model.Piece;
import src.model.Position;
import src.net.Protocol;
import src.net.client.NetworkGameProxy;
import src.net.messages.StateMessage;
import src.view.GameSnapshot;
import src.view.PieceSnapshot;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkGameProxyReplyOrderingTest {

    /**
     * Accepts one live connection and never replies to anything, so NetworkGameProxy.send() has a real
     * socket to write to (it throws on an unconnected client) while the test drives every reply itself,
     * deterministically, via direct onMessage(...) calls.
     */
    private static class SilentServer extends WebSocketServer {
        SilentServer(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
        }

        @Override
        public void onStart() {
        }
    }

    @Test
    public void testLateReplyToTimedOutRequestDoesNotCorruptNextRequest() throws Exception {
        SilentServer server = new SilentServer(new InetSocketAddress("localhost", 0));
        server.start();
        NetworkGameProxy proxy = null;
        try {
            int port = waitForBoundPort(server);
            proxy = new NetworkGameProxy(URI.create("ws://localhost:" + port), 100);
            assertTrue(proxy.connectBlocking(5, TimeUnit.SECONDS));

            PieceSnapshot[][] board = new PieceSnapshot[8][8];
            board[7][0] = new PieceSnapshot("p0", Piece.Color.WHITE, Piece.Kind.ROOK, Piece.State.IDLE, 0, 0, 0L, 0L);
            board[7][1] = new PieceSnapshot("p1", Piece.Color.WHITE, Piece.Kind.ROOK, Piece.State.IDLE, 0, 0, 0L, 0L);
            GameSnapshot snapshot = new GameSnapshot(8, 8, board, List.of(), Set.of(), false, null, 0, 0,
                    List.of(), List.of(), 1.0);
            proxy.onMessage(Protocol.encode(new StateMessage(snapshot)));

            MoveResult resultA = proxy.requestMove(new Position(7, 0), new Position(6, 0));
            assertFalse(resultA.isAccepted());
            assertEquals("timeout", resultA.reason());

            proxy.onMessage("OK");

            NetworkGameProxy finalProxy = proxy;
            Thread replier = new Thread(() -> {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {
                }
                finalProxy.onMessage("REJECT resting");
            });
            replier.start();
            MoveResult resultB = proxy.requestMove(new Position(7, 1), new Position(6, 1));
            replier.join();

            assertFalse(resultB.isAccepted());
            assertEquals("resting", resultB.reason());
        } finally {
            if (proxy != null) {
                proxy.closeBlocking();
            }
            server.stop();
        }
    }

    private int waitForBoundPort(WebSocketServer server) throws InterruptedException {
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
