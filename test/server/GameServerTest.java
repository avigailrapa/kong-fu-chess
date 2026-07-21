package server;

import org.java_websocket.WebSocket;
import org.junit.jupiter.api.Test;
import src.engine.GameEngine;
import src.engine.GameOverEvent;
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
import src.server.Session;
import src.server.UserStore;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GameServerTest {

    private RuleEngine rookOnlyRuleEngine() {
        Map<Piece.Kind, PieceRules> rulesByKind = Map.of(Piece.Kind.ROOK, new RookRule());
        return new RuleEngine(rulesByKind);
    }

    private record ServerAndMatch(GameServer server, Match match) {
    }

    private ServerAndMatch freshServerAndMatch() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0)), new Position(7, 0));
        GameEngine engine = new GameEngine(board, new GameState(), rookOnlyRuleEngine(), new RealTimeArbiter(board));
        Match match = new Match(engine, 1000);
        GameServer server = new GameServer(new InetSocketAddress("localhost", 0), match,
                new UserStore("jdbc:sqlite::memory:"));
        return new ServerAndMatch(server, match);
    }

    private GameServer freshServer() {
        return freshServerAndMatch().server();
    }

    private ServerAndMatch gameOverServerAndMatch() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0)), new Position(7, 0));
        GameState gameState = new GameState();
        gameState.endGame(Piece.Color.WHITE);
        GameEngine engine = new GameEngine(board, gameState, rookOnlyRuleEngine(), new RealTimeArbiter(board));
        Match match = new Match(engine, 1000);
        GameServer server = new GameServer(new InetSocketAddress("localhost", 0), match,
                new UserStore("jdbc:sqlite::memory:"));
        return new ServerAndMatch(server, match);
    }

    private void login(GameServer server, WebSocket conn, String username, Piece.Color expectedColor) {
        String reply = server.handleMessage(conn, "LOGIN " + username + " pw");
        assertEquals("WELCOME " + expectedColor.letter() + " 1200", reply);
    }

    @Test
    public void testAcceptedMoveReturnsOk() {
        GameServer server = freshServer();
        WebSocket conn = new FakeWebSocket();
        login(server, conn, "alice", Piece.Color.WHITE);

        String reply = server.handleMessage(conn, "WRa1a4");

        assertEquals("OK", reply);
    }

    @Test
    public void testDeclaredKindMismatchReturnsTokenMismatch() {
        GameServer server = freshServer();
        WebSocket conn = new FakeWebSocket();
        login(server, conn, "alice", Piece.Color.WHITE);

        String reply = server.handleMessage(conn, "WQa1a4");

        assertEquals("REJECT token_mismatch", reply);
    }

    @Test
    public void testDeclaredColorMismatchReturnsTokenMismatch() {
        GameServer server = freshServer();
        WebSocket white = new FakeWebSocket();
        login(server, white, "alice", Piece.Color.WHITE);
        WebSocket black = new FakeWebSocket();
        login(server, black, "bob", Piece.Color.BLACK);

        String reply = server.handleMessage(black, "BRa1a4");

        assertEquals("REJECT token_mismatch", reply);
    }

    @Test
    public void testMoveFromEmptySquareReturnsTokenMismatch() {
        GameServer server = freshServer();
        WebSocket conn = new FakeWebSocket();
        login(server, conn, "alice", Piece.Color.WHITE);

        String reply = server.handleMessage(conn, "WRa2a4");

        assertEquals("REJECT token_mismatch", reply);
    }

    @Test
    public void testIllegalDestinationReturnsEngineReason() {
        GameServer server = freshServer();
        WebSocket conn = new FakeWebSocket();
        login(server, conn, "alice", Piece.Color.WHITE);

        String reply = server.handleMessage(conn, "WRa1b7");

        assertEquals("REJECT illegal_piece_move", reply);
    }

    @Test
    public void testJumpAccepted() {
        GameServer server = freshServer();
        WebSocket conn = new FakeWebSocket();
        login(server, conn, "alice", Piece.Color.WHITE);

        String reply = server.handleMessage(conn, "JUMP WRa1");

        assertEquals("OK", reply);
    }

    @Test
    public void testJumpDeclaredMismatchReturnsTokenMismatch() {
        GameServer server = freshServer();
        WebSocket white = new FakeWebSocket();
        login(server, white, "alice", Piece.Color.WHITE);
        WebSocket black = new FakeWebSocket();
        login(server, black, "bob", Piece.Color.BLACK);

        String reply = server.handleMessage(black, "JUMP BRa1");

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
    public void testMoveWithWrongSeatColorIsRejectedNotYourPiece() {
        GameServer server = freshServer();
        login(server, new FakeWebSocket(), "alice", Piece.Color.WHITE);
        WebSocket black = new FakeWebSocket();
        login(server, black, "bob", Piece.Color.BLACK);

        String reply = server.handleMessage(black, "WRa1a4");

        assertEquals("REJECT not_your_piece", reply);
    }

    @Test
    public void testFirstThenSecondLoginAreSeatedWhiteThenBlack() {
        GameServer server = freshServer();

        assertEquals("WELCOME W 1200", server.handleMessage(new FakeWebSocket(), "LOGIN alice pw"));
        assertEquals("WELCOME B 1200", server.handleMessage(new FakeWebSocket(), "LOGIN bob pw"));
    }

    @Test
    public void testThirdLoginIsRejectedTableFull() {
        GameServer server = freshServer();
        login(server, new FakeWebSocket(), "alice", Piece.Color.WHITE);
        login(server, new FakeWebSocket(), "bob", Piece.Color.BLACK);

        String reply = server.handleMessage(new FakeWebSocket(), "LOGIN carol pw");

        assertEquals("REJECT table_full", reply);
    }

    @Test
    public void testLoginWithWrongPasswordIsRejectedBadCredentials() {
        GameServer server = freshServer();
        login(server, new FakeWebSocket(), "alice", Piece.Color.WHITE);

        String reply = server.handleMessage(new FakeWebSocket(), "LOGIN alice wrongpw");

        assertEquals("REJECT bad_credentials", reply);
    }

    @Test
    public void testReloginWithCorrectPasswordSucceedsWithPersistedRating() {
        GameServer server = freshServer();
        login(server, new FakeWebSocket(), "alice", Piece.Color.WHITE);

        String reply = server.handleMessage(new FakeWebSocket(), "LOGIN alice pw");

        assertEquals("WELCOME B 1200", reply);
    }

    @Test
    public void testSelectUpdatesSessionSelectedCell() {
        ServerAndMatch sm = freshServerAndMatch();
        WebSocket conn = new FakeWebSocket();
        login(sm.server(), conn, "alice", Piece.Color.WHITE);

        String reply = sm.server().handleMessage(conn, "SELECT a1");

        assertEquals("OK", reply);
        assertEquals(new Position(7, 0), sm.match().seated().get(0).selectedCell());
    }

    @Test
    public void testSelectWithNoSquareClearsSessionSelectedCell() {
        ServerAndMatch sm = freshServerAndMatch();
        WebSocket conn = new FakeWebSocket();
        login(sm.server(), conn, "alice", Piece.Color.WHITE);
        sm.server().handleMessage(conn, "SELECT a1");

        String reply = sm.server().handleMessage(conn, "SELECT -");

        assertEquals("OK", reply);
        assertNull(sm.match().seated().get(0).selectedCell());
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
    public void testNewGameWhileGameInProgressIsRejected() {
        GameServer server = freshServer();
        WebSocket conn = new FakeWebSocket();
        login(server, conn, "alice", Piece.Color.WHITE);

        String reply = server.handleMessage(conn, "NEWGAME");

        assertEquals("REJECT game_in_progress", reply);
    }

    @Test
    public void testNewGameAfterGameOverIsAcceptedAndResetsTheBoard() {
        ServerAndMatch sm = gameOverServerAndMatch();
        WebSocket conn = new FakeWebSocket();
        login(sm.server(), conn, "alice", Piece.Color.WHITE);

        String reply = sm.server().handleMessage(conn, "NEWGAME");

        assertEquals("OK", reply);
        assertFalse(sm.match().engine().snapshot(null).gameOver());
        assertEquals("OK", sm.server().handleMessage(conn, "WNb1a3"));
    }

    @Test
    public void testMoveIsRejectedGameOverBeforeNewGameIsRequested() {
        ServerAndMatch sm = gameOverServerAndMatch();
        WebSocket conn = new FakeWebSocket();
        login(sm.server(), conn, "alice", Piece.Color.WHITE);

        String reply = sm.server().handleMessage(conn, "WRa1a4");

        assertEquals("REJECT game_over", reply);
    }

    @Test
    public void testGameOverUpdatesBothRatingsAndSendsEachPlayerTheirOwn() {
        ServerAndMatch sm = freshServerAndMatch();
        List<String> whiteMessages = new ArrayList<>();
        List<String> blackMessages = new ArrayList<>();
        sm.match().addSession(new Session(whiteMessages::add, "alice", Piece.Color.WHITE, 1200));
        sm.match().addSession(new Session(blackMessages::add, "bob", Piece.Color.BLACK, 1200));

        sm.match().engine().eventBus().publish(new GameOverEvent(Piece.Color.WHITE));

        assertEquals(List.of("RATING 1216"), whiteMessages);
        assertEquals(List.of("RATING 1184"), blackMessages);
    }
}
