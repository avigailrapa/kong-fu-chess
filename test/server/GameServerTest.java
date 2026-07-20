package server;

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

    private GameServer freshServer() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0)), new Position(7, 0));
        GameEngine engine = new GameEngine(board, new GameState(), rookOnlyRuleEngine(), new RealTimeArbiter(board));
        Match match = new Match(engine, 1000);
        return new GameServer(new InetSocketAddress("localhost", 0), match);
    }

    @Test
    public void testAcceptedMoveReturnsOk() {
        GameServer server = freshServer();

        String reply = server.handleMessage("WRa1a4");

        assertEquals("OK", reply);
    }

    @Test
    public void testDeclaredKindMismatchReturnsTokenMismatch() {
        GameServer server = freshServer();

        String reply = server.handleMessage("WQa1a4");

        assertEquals("REJECT token_mismatch", reply);
    }

    @Test
    public void testDeclaredColorMismatchReturnsTokenMismatch() {
        GameServer server = freshServer();

        String reply = server.handleMessage("BRa1a4");

        assertEquals("REJECT token_mismatch", reply);
    }

    @Test
    public void testMoveFromEmptySquareReturnsTokenMismatch() {
        GameServer server = freshServer();

        String reply = server.handleMessage("WRa2a4");

        assertEquals("REJECT token_mismatch", reply);
    }

    @Test
    public void testIllegalDestinationReturnsEngineReason() {
        GameServer server = freshServer();

        String reply = server.handleMessage("WRa1b7");

        assertEquals("REJECT illegal_piece_move", reply);
    }

    @Test
    public void testJumpAccepted() {
        GameServer server = freshServer();

        String reply = server.handleMessage("JUMP WRa1");

        assertEquals("OK", reply);
    }

    @Test
    public void testJumpDeclaredMismatchReturnsTokenMismatch() {
        GameServer server = freshServer();

        String reply = server.handleMessage("JUMP BRa1");

        assertEquals("REJECT token_mismatch", reply);
    }

    @Test
    public void testMalformedMessageReturnsRejectMalformed() {
        GameServer server = freshServer();

        String reply = server.handleMessage("not a real message");

        assertEquals("REJECT malformed", reply);
    }

    @Test
    public void testServerToClientOnlyMessageTypeIsRejected() {
        GameServer server = freshServer();

        assertEquals("REJECT unexpected_message", server.handleMessage("OK"));
        assertEquals("REJECT unexpected_message", server.handleMessage("REJECT resting"));
    }
}
