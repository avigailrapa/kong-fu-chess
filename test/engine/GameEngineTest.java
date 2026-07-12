package engine;

import model.Board;
import model.GameState;
import model.Piece;
import model.Position;
import org.junit.jupiter.api.Test;
import rules.PieceRules;
import rules.RookRule;
import rules.RuleEngine;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GameEngineTest {

    private RuleEngine ruleEngine() {
        Map<Piece.Kind, PieceRules> rulesByKind = Map.of(Piece.Kind.ROOK, new RookRule());
        return new RuleEngine(rulesByKind);
    }

    @Test
    public void testLegalMoveIsAccepted() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        MoveResult result = engine.requestMove(new Position(7, 0), new Position(4, 0));

        assertTrue(result.isAccepted());
        assertEquals("ok", result.reason());
    }

    @Test
    public void testRequestMoveOnlyStartsMotionDoesNotMoveImmediately() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 0), new Position(4, 0));

        assertTrue(board.getPieceAt(new Position(7, 0)).isPresent());
        assertTrue(board.getPieceAt(new Position(4, 0)).isEmpty());
        assertEquals(Piece.State.MOVING, rook.getState());
    }

    @Test
    public void testIllegalMoveIsRejectedWithRuleEngineReason() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        MoveResult result = engine.requestMove(new Position(7, 0), new Position(5, 3));

        assertFalse(result.isAccepted());
        assertEquals("illegal_piece_move", result.reason());
    }

    @Test
    public void testGameOverGuardShortCircuitsBeforeRuleEngine() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        GameState gameState = new GameState();
        gameState.endGame();
        GameEngine engine = new GameEngine(board, gameState, ruleEngine(), new RealTimeArbiter(board));

        MoveResult result = engine.requestMove(new Position(7, 0), new Position(4, 0));

        assertFalse(result.isAccepted());
        assertEquals("game_over", result.reason());
    }

    @Test
    public void testSecondMoveRejectedWhileMotionInProgress() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece otherRook = new Piece("r2", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 7));
        board.addPiece(otherRook, new Position(7, 7));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 0), new Position(4, 0));
        MoveResult result = engine.requestMove(new Position(7, 7), new Position(4, 7));

        assertFalse(result.isAccepted());
        assertEquals("motion_in_progress", result.reason());
    }

    @Test
    public void testWaitMsResolvesArrivalAndMovesPieceOnBoard() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 0), new Position(4, 0));
        engine.waitMs(3000);

        assertTrue(board.getPieceAt(new Position(7, 0)).isEmpty());
        assertTrue(board.getPieceAt(new Position(4, 0)).isPresent());
        assertEquals(Piece.State.IDLE, rook.getState());
    }

    @Test
    public void testNewMoveAcceptedAfterPreviousMotionResolved() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 0), new Position(4, 0));
        engine.waitMs(3000);
        MoveResult result = engine.requestMove(new Position(4, 0), new Position(4, 4));

        assertTrue(result.isAccepted());
        assertEquals("ok", result.reason());
    }
}
