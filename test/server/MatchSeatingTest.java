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
import src.server.Match;
import src.server.Session;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class MatchSeatingTest {

    private RuleEngine rookOnlyRuleEngine() {
        Map<Piece.Kind, PieceRules> rulesByKind = Map.of(Piece.Kind.ROOK, new RookRule());
        return new RuleEngine(rulesByKind);
    }

    private Match freshMatch() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0)), new Position(7, 0));
        GameEngine engine = new GameEngine(board, new GameState(), rookOnlyRuleEngine(), new RealTimeArbiter(board));
        return new Match(engine, 1000);
    }

    @Test
    public void testFirstConnectionIsAssignedWhite() {
        Match match = freshMatch();

        assertEquals(Optional.of(Piece.Color.WHITE), match.assignSeat());
    }

    @Test
    public void testSecondConnectionIsAssignedBlack() {
        Match match = freshMatch();
        match.addSession(new Session(null, "alice", Piece.Color.WHITE));

        assertEquals(Optional.of(Piece.Color.BLACK), match.assignSeat());
    }

    @Test
    public void testThirdConnectionFindsTableFull() {
        Match match = freshMatch();
        match.addSession(new Session(null, "alice", Piece.Color.WHITE));
        match.addSession(new Session(null, "bob", Piece.Color.BLACK));

        assertEquals(Optional.empty(), match.assignSeat());
    }

    @Test
    public void testAddSessionRegistersInSeatedList() {
        Match match = freshMatch();
        Session alice = new Session(null, "alice", Piece.Color.WHITE);

        match.addSession(alice);

        assertEquals(1, match.seated().size());
        assertSame(alice, match.seated().get(0));
    }
}
