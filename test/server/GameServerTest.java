package server;

import org.java_websocket.WebSocket;
import org.junit.jupiter.api.Test;
import src.engine.GameOverEvent;
import src.model.Piece;
import src.model.Position;
import src.net.client.RoomCreateResult;
import src.net.client.RoomJoinResult;
import src.server.ActivityLog;
import src.server.GameServer;
import src.server.Match;
import src.server.UserStore;

import java.io.File;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

public class GameServerTest {

    private record ServerAndStore(GameServer server, UserStore userStore) {
    }

    private ServerAndStore freshServerAndStore() {
        UserStore userStore = new UserStore("jdbc:sqlite::memory:");
        ActivityLog activityLog = tempActivityLog();
        GameServer server = new GameServer(new InetSocketAddress("localhost", 0), userStore, 1000, 20, activityLog);
        return new ServerAndStore(server, userStore);
    }

    private ActivityLog tempActivityLog() {
        try {
            return new ActivityLog(File.createTempFile("kongfu-activity", ".log").getAbsolutePath());
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private GameServer freshServer() {
        return freshServerAndStore().server();
    }

    private void login(GameServer server, WebSocket conn, String username) {
        assertEquals("WELCOME 1200", server.handleMessage(conn, "LOGIN " + username + " pw"));
    }

    private record PairedMatch(WebSocket whiteConn, WebSocket blackConn, Match match) {
    }

    private PairedMatch pairMatch(GameServer server) {
        WebSocket whiteConn = new FakeWebSocket();
        WebSocket blackConn = new FakeWebSocket();
        login(server, whiteConn, "alice");
        login(server, blackConn, "bob");
        assertEquals("OK", server.handleMessage(whiteConn, "PLAY"));
        assertEquals("OK", server.handleMessage(blackConn, "PLAY"));
        return new PairedMatch(whiteConn, blackConn, server.matchFor(whiteConn));
    }

    @Test
    public void testAcceptedMoveReturnsOk() {
        GameServer server = freshServer();
        PairedMatch paired = pairMatch(server);

        String reply = server.handleMessage(paired.whiteConn(), "WPe2e4");

        assertEquals("OK", reply);
    }

    @Test
    public void testDeclaredKindMismatchReturnsTokenMismatch() {
        GameServer server = freshServer();
        PairedMatch paired = pairMatch(server);

        String reply = server.handleMessage(paired.whiteConn(), "WQa1a4");

        assertEquals("REJECT token_mismatch", reply);
    }

    @Test
    public void testDeclaredColorMismatchReturnsTokenMismatch() {
        GameServer server = freshServer();
        PairedMatch paired = pairMatch(server);

        String reply = server.handleMessage(paired.blackConn(), "BRa1a4");

        assertEquals("REJECT token_mismatch", reply);
    }

    @Test
    public void testMoveFromEmptySquareReturnsTokenMismatch() {
        GameServer server = freshServer();
        PairedMatch paired = pairMatch(server);

        String reply = server.handleMessage(paired.whiteConn(), "WRe4e5");

        assertEquals("REJECT token_mismatch", reply);
    }

    @Test
    public void testIllegalDestinationReturnsEngineReason() {
        GameServer server = freshServer();
        PairedMatch paired = pairMatch(server);

        String reply = server.handleMessage(paired.whiteConn(), "WRa1b7");

        assertEquals("REJECT illegal_piece_move", reply);
    }

    @Test
    public void testJumpAccepted() {
        GameServer server = freshServer();
        PairedMatch paired = pairMatch(server);

        String reply = server.handleMessage(paired.whiteConn(), "JUMP WRa1");

        assertEquals("OK", reply);
    }

    @Test
    public void testJumpDeclaredMismatchReturnsTokenMismatch() {
        GameServer server = freshServer();
        PairedMatch paired = pairMatch(server);

        String reply = server.handleMessage(paired.blackConn(), "JUMP BRa1");

        assertEquals("REJECT token_mismatch", reply);
    }

    @Test
    public void testMalformedMessageReturnsRejectMalformed() {
        GameServer server = freshServer();

        String reply = server.handleMessage(new FakeWebSocket(), "not a real message");

        assertEquals("REJECT malformed", reply);
    }

    @Test
    public void testServerToClientOnlyMessageTypeIsRejected() {
        GameServer server = freshServer();
        WebSocket conn = new FakeWebSocket();

        assertEquals("REJECT unexpected_message", server.handleMessage(conn, "OK"));
        assertEquals("REJECT unexpected_message", server.handleMessage(conn, "REJECT resting"));
    }

    @Test
    public void testMoveWithoutLoginIsRejectedNotYourPiece() {
        GameServer server = freshServer();

        String reply = server.handleMessage(new FakeWebSocket(), "WRa1a4");

        assertEquals("REJECT not_your_piece", reply);
    }

    @Test
    public void testMoveWhileNotInMatchIsRejectedNotYourPiece() {
        GameServer server = freshServer();
        WebSocket conn = new FakeWebSocket();
        login(server, conn, "alice");

        String reply = server.handleMessage(conn, "WRa1a4");

        assertEquals("REJECT not_your_piece", reply);
    }

    @Test
    public void testMoveWithWrongSeatColorIsRejectedNotYourPiece() {
        GameServer server = freshServer();
        PairedMatch paired = pairMatch(server);

        String reply = server.handleMessage(paired.blackConn(), "WRa1a4");

        assertEquals("REJECT not_your_piece", reply);
    }

    @Test
    public void testLoginReturnsWelcomeWithDefaultRating() {
        GameServer server = freshServer();

        String reply = server.handleMessage(new FakeWebSocket(), "LOGIN alice pw");

        assertEquals("WELCOME 1200", reply);
    }

    @Test
    public void testLoginWithWrongPasswordIsRejectedBadCredentials() {
        GameServer server = freshServer();
        login(server, new FakeWebSocket(), "alice");

        String reply = server.handleMessage(new FakeWebSocket(), "LOGIN alice wrongpw");

        assertEquals("REJECT bad_credentials", reply);
    }

    @Test
    public void testReloginWithCorrectPasswordSucceeds() {
        GameServer server = freshServer();
        login(server, new FakeWebSocket(), "alice");

        String reply = server.handleMessage(new FakeWebSocket(), "LOGIN alice pw");

        assertEquals("WELCOME 1200", reply);
    }

    @Test
    public void testFirstThenSecondPlayerToPlayAreSeatedWhiteThenBlack() {
        GameServer server = freshServer();
        PairedMatch paired = pairMatch(server);

        assertEquals(Piece.Color.WHITE, paired.match().seated().stream()
                .filter(s -> s.username().equals("alice")).findFirst().orElseThrow().assignedColor());
        assertEquals(Piece.Color.BLACK, paired.match().seated().stream()
                .filter(s -> s.username().equals("bob")).findFirst().orElseThrow().assignedColor());
    }

    @Test
    public void testThirdAndFourthPlayersFormASeparateMatch() {
        GameServer server = freshServer();
        PairedMatch firstPair = pairMatch(server);

        WebSocket carolConn = new FakeWebSocket();
        WebSocket daveConn = new FakeWebSocket();
        login(server, carolConn, "carol");
        login(server, daveConn, "dave");
        server.handleMessage(carolConn, "PLAY");
        server.handleMessage(daveConn, "PLAY");

        Match secondMatch = server.matchFor(carolConn);
        assertNotNull(secondMatch);
        assertNotSame(firstPair.match(), secondMatch);
    }

    @Test
    public void testPlayWithoutLoginIsRejectedNotLoggedIn() {
        GameServer server = freshServer();

        String reply = server.handleMessage(new FakeWebSocket(), "PLAY");

        assertEquals("REJECT not_logged_in", reply);
    }

    @Test
    public void testPlayWhileAlreadyInMatchIsRejected() {
        GameServer server = freshServer();
        PairedMatch paired = pairMatch(server);

        String reply = server.handleMessage(paired.whiteConn(), "PLAY");

        assertEquals("REJECT already_in_match", reply);
    }

    @Test
    public void testSelectUpdatesSessionSelectedCell() {
        GameServer server = freshServer();
        PairedMatch paired = pairMatch(server);

        String reply = server.handleMessage(paired.whiteConn(), "SELECT a1");

        assertEquals("OK", reply);
        assertEquals(new Position(7, 0), paired.match().seated().stream()
                .filter(s -> s.username().equals("alice")).findFirst().orElseThrow().selectedCell());
    }

    @Test
    public void testSelectWithNoSquareClearsSessionSelectedCell() {
        GameServer server = freshServer();
        PairedMatch paired = pairMatch(server);
        server.handleMessage(paired.whiteConn(), "SELECT a1");

        String reply = server.handleMessage(paired.whiteConn(), "SELECT -");

        assertEquals("OK", reply);
        assertNull(paired.match().seated().stream()
                .filter(s -> s.username().equals("alice")).findFirst().orElseThrow().selectedCell());
    }

    @Test
    public void testSelectWithoutLoginIsAcceptedButNotStored() {
        GameServer server = freshServer();

        String reply = server.handleMessage(new FakeWebSocket(), "SELECT a1");

        assertEquals("OK", reply);
    }

    @Test
    public void testNewGameWithoutLoginIsRejectedNotLoggedIn() {
        GameServer server = freshServer();

        String reply = server.handleMessage(new FakeWebSocket(), "NEWGAME");

        assertEquals("REJECT not_logged_in", reply);
    }

    @Test
    public void testNewGameWhileNotInMatchIsRejected() {
        GameServer server = freshServer();
        WebSocket conn = new FakeWebSocket();
        login(server, conn, "alice");

        String reply = server.handleMessage(conn, "NEWGAME");

        assertEquals("REJECT not_in_match", reply);
    }

    @Test
    public void testNewGameWhileGameInProgressIsRejected() {
        GameServer server = freshServer();
        PairedMatch paired = pairMatch(server);

        String reply = server.handleMessage(paired.whiteConn(), "NEWGAME");

        assertEquals("REJECT game_in_progress", reply);
    }

    @Test
    public void testNewGameAfterGameOverIsAcceptedAndResetsTheBoard() {
        GameServer server = freshServer();
        PairedMatch paired = pairMatch(server);
        paired.match().engine().resign(Piece.Color.WHITE);

        String reply = server.handleMessage(paired.whiteConn(), "NEWGAME");

        assertEquals("OK", reply);
        assertFalse(paired.match().engine().snapshot(null).gameOver());
        assertEquals("OK", server.handleMessage(paired.whiteConn(), "WNb1a3"));
    }

    @Test
    public void testMoveIsRejectedGameOverBeforeNewGameIsRequested() {
        GameServer server = freshServer();
        PairedMatch paired = pairMatch(server);
        paired.match().engine().resign(Piece.Color.WHITE);

        String reply = server.handleMessage(paired.whiteConn(), "WPe2e4");

        assertEquals("REJECT game_over", reply);
    }

    @Test
    public void testGameOverUpdatesBothRatingsAndPersistsThem() {
        ServerAndStore ss = freshServerAndStore();
        PairedMatch paired = pairMatch(ss.server());

        paired.match().engine().eventBus().publish(new GameOverEvent(Piece.Color.WHITE));

        assertEquals(1216, ss.userStore().find("alice").orElseThrow().rating());
        assertEquals(1184, ss.userStore().find("bob").orElseThrow().rating());
    }

    @Test
    public void testRoomCreateReturnsRoomId() {
        GameServer server = freshServer();
        WebSocket conn = new FakeWebSocket();
        login(server, conn, "alice");

        String reply = server.handleMessage(conn, "ROOM_CREATE");

        assertTrue(reply.startsWith("ROOM_ID "), "expected a ROOM_ID reply, got: " + reply);
    }

    @Test
    public void testRoomCreateWithoutLoginIsRejectedNotLoggedIn() {
        GameServer server = freshServer();

        String reply = server.handleMessage(new FakeWebSocket(), "ROOM_CREATE");

        assertEquals("REJECT not_logged_in", reply);
    }

    @Test
    public void testSecondPlayerJoiningRoomIsSeatedBlack() {
        GameServer server = freshServer();
        WebSocket creatorConn = new FakeWebSocket();
        WebSocket joinerConn = new FakeWebSocket();
        login(server, creatorConn, "alice");
        login(server, joinerConn, "bob");
        String roomId = roomIdFrom(server.handleMessage(creatorConn, "ROOM_CREATE"));

        String reply = server.handleMessage(joinerConn, "ROOM_JOIN " + roomId);

        assertEquals("ROOM_ID " + roomId, reply);
        assertEquals(Piece.Color.BLACK, server.matchFor(joinerConn).seated().stream()
                .filter(s -> s.username().equals("bob")).findFirst().orElseThrow().assignedColor());
    }

    @Test
    public void testThirdPlayerJoiningRoomIsSpectating() {
        GameServer server = freshServer();
        WebSocket creatorConn = new FakeWebSocket();
        WebSocket joinerConn = new FakeWebSocket();
        WebSocket spectatorConn = new FakeWebSocket();
        login(server, creatorConn, "alice");
        login(server, joinerConn, "bob");
        login(server, spectatorConn, "carol");
        String roomId = roomIdFrom(server.handleMessage(creatorConn, "ROOM_CREATE"));
        server.handleMessage(joinerConn, "ROOM_JOIN " + roomId);

        String reply = server.handleMessage(spectatorConn, "ROOM_JOIN " + roomId);

        assertEquals("SPECTATING", reply);
    }

    @Test
    public void testJoiningNonexistentRoomReturnsRoomNotFound() {
        GameServer server = freshServer();
        WebSocket conn = new FakeWebSocket();
        login(server, conn, "alice");

        String reply = server.handleMessage(conn, "ROOM_JOIN NOSUCHROOM");

        assertEquals("REJECT room_not_found", reply);
    }

    @Test
    public void testSpectatorMoveIsRejected() {
        GameServer server = freshServer();
        WebSocket creatorConn = new FakeWebSocket();
        WebSocket joinerConn = new FakeWebSocket();
        WebSocket spectatorConn = new FakeWebSocket();
        login(server, creatorConn, "alice");
        login(server, joinerConn, "bob");
        login(server, spectatorConn, "carol");
        String roomId = roomIdFrom(server.handleMessage(creatorConn, "ROOM_CREATE"));
        server.handleMessage(joinerConn, "ROOM_JOIN " + roomId);
        server.handleMessage(spectatorConn, "ROOM_JOIN " + roomId);

        String reply = server.handleMessage(spectatorConn, "WPe2e4");

        assertEquals("REJECT spectator", reply);
    }

    @Test
    public void testSpectatorNewGameIsRejected() {
        GameServer server = freshServer();
        WebSocket creatorConn = new FakeWebSocket();
        WebSocket joinerConn = new FakeWebSocket();
        WebSocket spectatorConn = new FakeWebSocket();
        login(server, creatorConn, "alice");
        login(server, joinerConn, "bob");
        login(server, spectatorConn, "carol");
        String roomId = roomIdFrom(server.handleMessage(creatorConn, "ROOM_CREATE"));
        server.handleMessage(joinerConn, "ROOM_JOIN " + roomId);
        server.handleMessage(spectatorConn, "ROOM_JOIN " + roomId);

        String reply = server.handleMessage(spectatorConn, "NEWGAME");

        assertEquals("REJECT spectator", reply);
    }

    private String roomIdFrom(String roomIdReply) {
        return roomIdReply.substring("ROOM_ID ".length());
    }

    @Test
    public void testReconnectWithinCountdownCancelsAutoResignAndResumesMatch() throws InterruptedException {
        ActivityLog activityLog = tempActivityLog();
        GameServer server = new GameServer(new InetSocketAddress("localhost", 0),
                new UserStore("jdbc:sqlite::memory:"), 1000, 2, activityLog);
        WebSocket whiteConn = new FakeWebSocket();
        WebSocket blackConn = new FakeWebSocket();
        login(server, whiteConn, "alice");
        login(server, blackConn, "bob");
        server.handleMessage(whiteConn, "PLAY");
        server.handleMessage(blackConn, "PLAY");
        Match match = server.matchFor(whiteConn);

        server.onClose(whiteConn, 1000, "test", false);
        WebSocket reconnectConn = new FakeWebSocket();
        String reply = server.handleMessage(reconnectConn, "LOGIN alice pw");

        assertEquals("WELCOME_BACK 1200", reply);
        assertSame(match, server.matchFor(reconnectConn));

        Thread.sleep(2500);
        assertFalse(match.engine().snapshot(null).gameOver());
    }

    @Test
    public void testReconnectWithWrongPasswordIsRejectedAndCountdownContinues() throws InterruptedException {
        ActivityLog activityLog = tempActivityLog();
        GameServer server = new GameServer(new InetSocketAddress("localhost", 0),
                new UserStore("jdbc:sqlite::memory:"), 1000, 1, activityLog);
        WebSocket whiteConn = new FakeWebSocket();
        WebSocket blackConn = new FakeWebSocket();
        login(server, whiteConn, "alice");
        login(server, blackConn, "bob");
        server.handleMessage(whiteConn, "PLAY");
        server.handleMessage(blackConn, "PLAY");
        Match match = server.matchFor(whiteConn);

        server.onClose(whiteConn, 1000, "test", false);
        String reply = server.handleMessage(new FakeWebSocket(), "LOGIN alice wrongpw");

        assertEquals("REJECT bad_credentials", reply);

        Thread.sleep(1500);
        assertTrue(match.engine().snapshot(null).gameOver());
    }

    @Test
    public void testDisconnectedSpectatorDoesNotStartResignCountdown() throws InterruptedException {
        ActivityLog activityLog = tempActivityLog();
        GameServer server = new GameServer(new InetSocketAddress("localhost", 0),
                new UserStore("jdbc:sqlite::memory:"), 1000, 1, activityLog);
        WebSocket creatorConn = new FakeWebSocket();
        WebSocket joinerConn = new FakeWebSocket();
        WebSocket spectatorConn = new FakeWebSocket();
        login(server, creatorConn, "alice");
        login(server, joinerConn, "bob");
        login(server, spectatorConn, "carol");
        String roomId = roomIdFrom(server.handleMessage(creatorConn, "ROOM_CREATE"));
        server.handleMessage(joinerConn, "ROOM_JOIN " + roomId);
        server.handleMessage(spectatorConn, "ROOM_JOIN " + roomId);
        Match match = server.matchFor(creatorConn);

        server.onClose(spectatorConn, 1000, "test", false);
        Thread.sleep(1500);

        assertFalse(match.engine().snapshot(null).gameOver());
    }
}
