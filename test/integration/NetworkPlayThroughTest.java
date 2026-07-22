package integration;

import org.junit.jupiter.api.Test;
import src.engine.MoveResult;
import src.model.Piece;
import src.model.Position;
import src.net.client.LoginResult;
import src.net.client.NetworkGameProxy;
import src.net.client.RoomCreateResult;
import src.net.client.RoomJoinResult;
import src.server.ActivityLog;
import src.server.GameServer;
import src.server.UserStore;
import src.view.GameSnapshot;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkPlayThroughTest {

    private ActivityLog tempActivityLog() throws Exception {
        return new ActivityLog(File.createTempFile("kongfu-activity", ".log").getAbsolutePath());
    }

    @Test
    public void testMoveRequestedThroughProxyIsAcceptedAndSnapshotUpdates() throws Exception {
        GameServer server = new GameServer(new InetSocketAddress("localhost", 0),
                new UserStore("jdbc:sqlite::memory:"), 100, 20, tempActivityLog());
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
        GameServer server = new GameServer(new InetSocketAddress("localhost", 0), userStore, 100, 1, tempActivityLog());
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

    @Test
    public void testRoomCreateJoinAndSpectateFlow() throws Exception {
        GameServer server = new GameServer(new InetSocketAddress("localhost", 0),
                new UserStore("jdbc:sqlite::memory:"), 100, 20, tempActivityLog());
        server.start();

        int port = waitForBoundPort(server);
        NetworkGameProxy creator = new NetworkGameProxy(URI.create("ws://localhost:" + port), 5000);
        NetworkGameProxy joiner = new NetworkGameProxy(URI.create("ws://localhost:" + port), 5000);
        NetworkGameProxy spectator = new NetworkGameProxy(URI.create("ws://localhost:" + port), 5000);
        NetworkGameProxy stranger = new NetworkGameProxy(URI.create("ws://localhost:" + port), 5000);
        try {
            assertTrue(creator.connectBlocking(5, TimeUnit.SECONDS));
            assertTrue(joiner.connectBlocking(5, TimeUnit.SECONDS));
            assertTrue(spectator.connectBlocking(5, TimeUnit.SECONDS));
            assertTrue(stranger.connectBlocking(5, TimeUnit.SECONDS));

            creator.login("alice", "pw");
            joiner.login("bob", "pw");
            spectator.login("carol", "pw");
            stranger.login("dave", "pw");

            RoomCreateResult created = creator.createRoom();
            assertTrue(created.accepted(), "expected room creation to be accepted: " + created.reason());
            assertNotNull(created.roomId());

            RoomJoinResult joined = joiner.joinRoom(created.roomId());
            assertTrue(joined.accepted(), "expected join to be accepted: " + joined.reason());
            assertFalse(joined.spectating(), "the second player should be seated, not spectating");

            waitUntil(() -> creator.latestSnapshot() != null && joiner.latestSnapshot() != null,
                    "both players to receive an initial STATE after the room fills");

            RoomJoinResult spectating = spectator.joinRoom(created.roomId());
            assertTrue(spectating.accepted(), "expected spectate to be accepted: " + spectating.reason());
            assertTrue(spectating.spectating(), "a third joiner should become a spectator");

            waitUntil(() -> spectator.latestSnapshot() != null, "the spectator to receive an initial STATE");
            assertTrue(spectator.isOccupied(new Position(7, 1)));

            MoveResult spectatorMove = spectator.requestMove(new Position(7, 1), new Position(5, 1));
            assertFalse(spectatorMove.isAccepted());
            assertEquals("spectator", spectatorMove.reason());

            RoomJoinResult notFound = stranger.joinRoom("NOSUCHROOM");
            assertFalse(notFound.accepted());
            assertEquals("room_not_found", notFound.reason());
        } finally {
            creator.closeBlocking();
            joiner.closeBlocking();
            spectator.closeBlocking();
            stranger.closeBlocking();
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
