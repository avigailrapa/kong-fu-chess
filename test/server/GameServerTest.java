package server;

import org.java_websocket.WebSocket;
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

import java.net.InetSocketAddress;
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
        GameServer server = new GameServer(new InetSocketAddress("localhost", 0), match);
        return new ServerAndMatch(server, match);
    }

    private GameServer freshServer() {
        return freshServerAndMatch().server();
    }

    private void login(GameServer server, WebSocket conn, String username, Piece.Color expectedColor) {
        String reply = server.handleMessage(conn, "LOGIN " + username);
        assertEquals("WELCOME " + expectedColor.letter(), reply);
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

        assertEquals("WELCOME W", server.handleMessage(new FakeWebSocket(), "LOGIN alice"));
        assertEquals("WELCOME B", server.handleMessage(new FakeWebSocket(), "LOGIN bob"));
    }

    @Test
    public void testThirdLoginIsRejectedTableFull() {
        GameServer server = freshServer();
        login(server, new FakeWebSocket(), "alice", Piece.Color.WHITE);
        login(server, new FakeWebSocket(), "bob", Piece.Color.BLACK);

        String reply = server.handleMessage(new FakeWebSocket(), "LOGIN carol");

        assertEquals("REJECT table_full", reply);
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
}
