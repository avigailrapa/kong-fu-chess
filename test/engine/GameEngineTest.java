package engine;
import src.engine.GameEngine;
import src.engine.GameSnapshot;
import src.engine.MoveResult;
import src.model.*;
import org.junit.jupiter.api.Test;
import src.realtime.*;
import src.rules.*;


import java.util.Map;
import java.util.Set;

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
    public void testSecondMoveForSameMovingPieceIsRejected() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 0), new Position(4, 0));
        MoveResult result = engine.requestMove(new Position(7, 0), new Position(6, 0));

        assertFalse(result.isAccepted());
        assertEquals("motion_in_progress", result.reason());
    }

    @Test
    public void testDifferentPieceCanMoveWhileAnotherIsInMotion() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece otherRook = new Piece("r2", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 7));
        board.addPiece(otherRook, new Position(7, 7));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 0), new Position(4, 0));
        MoveResult result = engine.requestMove(new Position(7, 7), new Position(4, 7));

        assertTrue(result.isAccepted());
        assertEquals("ok", result.reason());
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

    @Test
    public void testCapturingEnemyKingEndsTheGame() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        board.addPiece(new Piece("k1", Piece.Color.BLACK, Piece.Kind.KING, new Position(4, 0)), new Position(4, 0));
        GameState gameState = new GameState();
        GameEngine engine = new GameEngine(board, gameState, ruleEngine(), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 0), new Position(4, 0));
        engine.waitMs(3000);

        assertTrue(gameState.isGameOver());
    }

    @Test
    public void testCapturingNonKingPieceDoesNotEndTheGame() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        board.addPiece(new Piece("q1", Piece.Color.BLACK, Piece.Kind.QUEEN, new Position(4, 0)), new Position(4, 0));
        GameState gameState = new GameState();
        GameEngine engine = new GameEngine(board, gameState, ruleEngine(), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 0), new Position(4, 0));
        engine.waitMs(3000);

        assertFalse(gameState.isGameOver());
    }

    @Test
    public void testRequestMoveRejectedAfterKingCaptureEndsGame() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        board.addPiece(new Piece("k1", Piece.Color.BLACK, Piece.Kind.KING, new Position(4, 0)), new Position(4, 0));
        Piece otherRook = new Piece("r2", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 7));
        board.addPiece(otherRook, new Position(7, 7));
        GameState gameState = new GameState();
        GameEngine engine = new GameEngine(board, gameState, ruleEngine(), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 0), new Position(4, 0));
        engine.waitMs(3000);
        MoveResult result = engine.requestMove(new Position(7, 7), new Position(4, 7));

        assertFalse(result.isAccepted());
        assertEquals("game_over", result.reason());
    }

    @Test
    public void testIllegalMoveLeavesBoardUnchangedEvenAfterWait() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 0), new Position(5, 3));
        engine.waitMs(5000);

        assertTrue(board.getPieceAt(new Position(7, 0)).isPresent());
        assertEquals(Piece.State.IDLE, rook.getState());
    }

    @Test
    public void testBlockedSlidingPathLeavesBoardUnchanged() {
        Board board = new Board(3, 3);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));
        board.addPiece(rook, new Position(0, 0));
        Piece blockingPawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(0, 1));
        board.addPiece(blockingPawn, new Position(0, 1));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        MoveResult result = engine.requestMove(new Position(0, 0), new Position(0, 2));
        engine.waitMs(3000);

        assertFalse(result.isAccepted());
        assertEquals("illegal_piece_move", result.reason());
        assertTrue(board.getPieceAt(new Position(0, 0)).isPresent());
        assertTrue(board.getPieceAt(new Position(0, 1)).isPresent());
        assertTrue(board.getPieceAt(new Position(0, 2)).isEmpty());
    }

    @Test
    public void testFriendlyDestinationLeavesBoardUnchanged() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece friendlyPawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(4, 0));
        board.addPiece(friendlyPawn, new Position(4, 0));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        MoveResult result = engine.requestMove(new Position(7, 0), new Position(4, 0));
        engine.waitMs(3000);

        assertFalse(result.isAccepted());
        assertEquals("friendly_destination", result.reason());
        assertTrue(board.getPieceAt(new Position(7, 0)).isPresent());
        assertTrue(board.getPieceAt(new Position(4, 0)).isPresent());
    }

    @Test
    public void testInvalidCommandDoesNotStartMotion() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece otherRook = new Piece("r2", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 7));
        board.addPiece(otherRook, new Position(7, 7));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 0), new Position(5, 3));
        MoveResult secondMove = engine.requestMove(new Position(7, 7), new Position(4, 7));

        assertTrue(secondMove.isAccepted());
        assertEquals("ok", secondMove.reason());
    }

    @Test
    public void testFromBoardWiresAllSixPieceRules() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(7, 2)), new Position(7, 2));
        board.addPiece(new Piece("n1", Piece.Color.WHITE, Piece.Kind.KNIGHT, new Position(7, 1)), new Position(7, 1));
        board.addPiece(new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(6, 4)), new Position(6, 4));
        GameEngine engine = GameEngine.fromBoard(board);

        MoveResult bishopMove = engine.requestMove(new Position(7, 2), new Position(5, 4));
        assertTrue(bishopMove.isAccepted());
        engine.waitMs(2000);
        assertTrue(board.getPieceAt(new Position(5, 4)).isPresent());

        MoveResult knightMove = engine.requestMove(new Position(7, 1), new Position(5, 2));
        assertTrue(knightMove.isAccepted());
        engine.waitMs(2000);
        assertTrue(board.getPieceAt(new Position(5, 2)).isPresent());

        MoveResult pawnMove = engine.requestMove(new Position(6, 4), new Position(5, 4));
        assertFalse(pawnMove.isAccepted());
        assertEquals("friendly_destination", pawnMove.reason());
    }

    @Test
    public void testSnapshotReflectsOccupiedCells() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0)), new Position(7, 0));
        board.addPiece(new Piece("k1", Piece.Color.BLACK, Piece.Kind.KING, new Position(0, 4)), new Position(0, 4));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        GameSnapshot snapshot = engine.snapshot();

        assertEquals(Set.of(new Position(7, 0), new Position(0, 4)), snapshot.occupiedCells());
        assertTrue(snapshot.isOccupied(new Position(7, 0)));
        assertFalse(snapshot.isOccupied(new Position(3, 3)));
    }

    @Test
    public void testSettledBoardReturnsSameBoardInstance() {
        Board board = new Board(8, 8);
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        assertSame(board, engine.settledBoard());
    }

    @Test
    public void testWaitMsWithNoActiveMotionIsNoOp() {
        Board board = new Board(8, 8);
        GameState gameState = new GameState();
        GameEngine engine = new GameEngine(board, gameState, ruleEngine(), new RealTimeArbiter(board));

        engine.waitMs(500);

        assertFalse(gameState.isGameOver());
    }

    @Test
    public void testWaitMsRejectsNegativeMilliseconds() {
        Board board = new Board(8, 8);
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        assertThrows(IllegalArgumentException.class, () -> engine.waitMs(-1));
    }

    @Test
    public void testGameOverGuardTakesPriorityOverMotionInProgress() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece enemyKing = new Piece("k1", Piece.Color.BLACK, Piece.Kind.KING, new Position(4, 0));
        board.addPiece(enemyKing, new Position(4, 0));
        Piece movingRook = new Piece("r2", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 7));
        board.addPiece(movingRook, new Position(7, 7));
        Map<Piece.Kind, PieceRules> rulesByKind = Map.of(Piece.Kind.ROOK, new RookRule());
        GameState gameState = new GameState();
        GameEngine engine = new GameEngine(board, gameState, new RuleEngine(rulesByKind), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 7), new Position(0, 7)); // movingRook needs 7000ms, stays in motion past the wait below
        engine.requestMove(new Position(7, 0), new Position(4, 0)); // captures the king, ends the game after 3000ms
        engine.waitMs(3000);
        assertTrue(gameState.isGameOver());

        MoveResult result = engine.requestMove(new Position(7, 7), new Position(6, 7));

        assertFalse(result.isAccepted());
        assertEquals("game_over", result.reason());
    }


    @Test
    public void testRequestJumpDoesNothingAfterGameOver() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece enemyKing = new Piece("k1", Piece.Color.BLACK, Piece.Kind.KING, new Position(4, 0));
        board.addPiece(enemyKing, new Position(4, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        GameState gameState = new GameState();
        GameEngine engine = new GameEngine(board, gameState, ruleEngine(), arbiter);
        engine.requestMove(new Position(7, 0), new Position(4, 0));
        engine.waitMs(3000);
        assertTrue(gameState.isGameOver());

        engine.requestJump(new Position(4, 0));

        assertFalse(arbiter.hasActiveJump());
    }

    @Test
    public void testRequestJumpOnEmptyCellDoesNothing() {
        Board board = new Board(8, 8);
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), arbiter);

        engine.requestJump(new Position(3, 3));

        assertFalse(arbiter.hasActiveJump());
    }
}
