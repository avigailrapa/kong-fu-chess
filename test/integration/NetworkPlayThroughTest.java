package integration;

import org.junit.jupiter.api.Test;
import src.engine.MoveResult;
import src.model.Piece;
import src.model.Position;
import src.net.LoginResult;
import src.net.NetworkGameProxy;
import src.server.GameServer;
import src.server.UserStore;
import src.view.GameSnapshot;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkPlayThroughTest {

    @Test
    public void testMoveRequestedThroughProxyIsAcceptedAndSnapshotUpdates() throws Exception {
        GameServer server = new GameServer(new InetSocketAddress("localhost", 0),
                new UserStore("jdbc:sqlite::memory:"), 100, 20);
        server.start();

        int port = waitForBoundPort(server);
        NetworkGameProxy white = new NetworkGameProxy(URI.create("ws://localhost:" + port), 5000);
        NetworkGameProxy black = new NetworkGameProxy(URI.create("ws://localhost:" + port), 5000);
        try {
            assertTrue(white.connectBlocking(5, TimeUnit.SECONDS));
            assertTrue(black.connectBlocking(5, TimeUnit.SECONDS));

            LoginResult whiteLogin = white.login("alice", "pw");
            LoginResult blackLogin = black.login("bob", "pw");
            assertTrue(whiteLogin.accepted(), "expected login to be accepted: " + whiteLogin.reason());
            assertTrue(blackLogin.accepted(), "expected login to be accepted: " + blackLogin.reason());

            white.play();
            black.play();
            waitUntil(() -> white.latestSnapshot() != null && black.latestSnapshot() != null,
                    "both clients to receive an initial STATE after pairing");
            assertTrue(white.isOccupied(new Position(7, 1)));

            MoveResult result = white.requestMove(new Position(7, 1), new Position(5, 2));

            assertTrue(result.isAccepted(), "expected the move to be accepted: " + result.reason());
            waitUntil(() -> pieceHasArrived(white, new Position(5, 2)), "the piece to arrive at its destination");
        } finally {
            white.closeBlocking();
            black.closeBlocking();
            server.stop();
        }
    }

    @Test
    public void testOpponentDisconnectResignsAndUpdatesBothRatings() throws Exception {
        UserStore userStore = new UserStore("jdbc:sqlite::memory:");
        GameServer server = new GameServer(new InetSocketAddress("localhost", 0), userStore, 100, 1);
        server.start();

        int port = waitForBoundPort(server);
        NetworkGameProxy white = new NetworkGameProxy(URI.create("ws://localhost:" + port), 5000);
        NetworkGameProxy black = new NetworkGameProxy(URI.create("ws://localhost:" + port), 5000);
        try {
            assertTrue(white.connectBlocking(5, TimeUnit.SECONDS));
            assertTrue(black.connectBlocking(5, TimeUnit.SECONDS));

            LoginResult whiteLogin = white.login("alice", "pw");
            LoginResult blackLogin = black.login("bob", "pw");
            assertEquals(1200, whiteLogin.rating());
            assertEquals(1200, blackLogin.rating());

            white.play();
            black.play();
            waitUntil(() -> white.latestSnapshot() != null && black.latestSnapshot() != null,
                    "both clients to receive an initial STATE after pairing");

            white.closeBlocking();

            waitUntil(() -> black.latestRating() != 1200,
                    "black's rating to change after white disconnects and resigns");
            assertTrue(black.latestRating() > 1200, "surviving player's rating should increase");
            assertEquals(black.latestRating(), userStore.find("bob").orElseThrow().rating());
            assertTrue(userStore.find("alice").orElseThrow().rating() < 1200,
                    "resigning player's persisted rating should decrease");
        } finally {
            black.closeBlocking();
            server.stop();
        }
    }

    private boolean pieceHasArrived(NetworkGameProxy proxy, Position destination) {
        GameSnapshot snapshot = proxy.latestSnapshot();
        return snapshot != null && snapshot.pieceAt(destination) != null;
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

    private void waitUntil(java.util.function.BooleanSupplier condition, String descriptionOfWhatWeWaitedFor)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("timed out waiting for " + descriptionOfWhatWeWaitedFor);
    }
}
