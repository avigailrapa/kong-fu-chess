package engine;
import src.engine.GameEngine;
import src.engine.MoveResult;
import src.view.snapshot.GameSnapshot;
import src.view.snapshot.PieceSnapshot;
import src.model.*;
import org.junit.jupiter.api.Test;
import src.realtime.*;
import src.rules.*;
import src.rules.pieces.*;


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

        assertTrue(board.pieceAt(new Position(7, 0)).isPresent());
        assertTrue(board.pieceAt(new Position(4, 0)).isEmpty());
        assertEquals(Piece.State.MOVING, rook.state());
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

        assertTrue(board.pieceAt(new Position(7, 0)).isEmpty());
        assertTrue(board.pieceAt(new Position(4, 0)).isPresent());
        assertEquals(Piece.State.LONG_REST, rook.state());
    }

    @Test
    public void testNewMoveAcceptedAfterPreviousMotionResolved() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 0), new Position(4, 0));
        engine.waitMs(3000);
        engine.waitMs(2000); 
        MoveResult result = engine.requestMove(new Position(4, 0), new Position(4, 4));

        assertTrue(result.isAccepted());
        assertEquals("ok", result.reason());
    }

    @Test
    public void testMoveRejectedWhilePieceIsRestingAfterItsPreviousMove() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 0), new Position(4, 0));
        engine.waitMs(3000); 

        MoveResult result = engine.requestMove(new Position(4, 0), new Position(4, 4));

        assertFalse(result.isAccepted());
        assertEquals("resting", result.reason());
    }

    @Test
    public void testMoveRejectedWhilePieceIsMidJump() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), arbiter);

        engine.requestJump(new Position(7, 0));
        MoveResult result = engine.requestMove(new Position(7, 0), new Position(4, 0));

        assertFalse(result.isAccepted());
        assertEquals("motion_in_progress", result.reason());
        assertTrue(arbiter.isJumping(rook));
        assertFalse(arbiter.isMoving(rook));
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

        assertTrue(gameState.gameOver());
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

        assertFalse(gameState.gameOver());
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

        assertTrue(board.pieceAt(new Position(7, 0)).isPresent());
        assertEquals(Piece.State.IDLE, rook.state());
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
        assertTrue(board.pieceAt(new Position(0, 0)).isPresent());
        assertTrue(board.pieceAt(new Position(0, 1)).isPresent());
        assertTrue(board.pieceAt(new Position(0, 2)).isEmpty());
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
        assertTrue(board.pieceAt(new Position(7, 0)).isPresent());
        assertTrue(board.pieceAt(new Position(4, 0)).isPresent());
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
        assertTrue(board.pieceAt(new Position(5, 4)).isPresent());

        MoveResult knightMove = engine.requestMove(new Position(7, 1), new Position(5, 2));
        assertTrue(knightMove.isAccepted());
        engine.waitMs(2000);
        assertTrue(board.pieceAt(new Position(5, 2)).isPresent());

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

        GameSnapshot snapshot = engine.snapshot(null);

        assertEquals(Piece.Kind.ROOK, snapshot.pieceAt(new Position(7, 0)).kind());
        assertEquals(Piece.Kind.KING, snapshot.pieceAt(new Position(0, 4)).kind());
        assertTrue(snapshot.isOccupied(new Position(7, 0)));
        assertFalse(snapshot.isOccupied(new Position(3, 3)));
    }

    @Test
    public void testWaitMsWithNoActiveMotionIsNoOp() {
        Board board = new Board(8, 8);
        GameState gameState = new GameState();
        GameEngine engine = new GameEngine(board, gameState, ruleEngine(), new RealTimeArbiter(board));

        engine.waitMs(500);

        assertFalse(gameState.gameOver());
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

        engine.requestMove(new Position(7, 7), new Position(0, 7)); 
        engine.requestMove(new Position(7, 0), new Position(4, 0)); 
        engine.waitMs(3000);
        assertTrue(gameState.gameOver());

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
        assertTrue(gameState.gameOver());

        engine.requestJump(new Position(4, 0));

        assertFalse(arbiter.isJumping(rook));
    }

    @Test
    public void testRequestJumpOnEmptyCellDoesNothing() {
        Board board = new Board(8, 8);
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), arbiter);

        engine.requestJump(new Position(3, 3));

        assertTrue(arbiter.isIdle());
    }

    @Test
    public void testOppositeColorRaceToSameCellDoesNotCrash() {
        Board board = new Board(8, 8);
        Piece whiteRook = new Piece("wr", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(4, 0));
        Piece blackRook = new Piece("br", Piece.Color.BLACK, Piece.Kind.ROOK, new Position(4, 7));
        board.addPiece(whiteRook, new Position(4, 0));
        board.addPiece(blackRook, new Position(4, 7));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        engine.requestMove(new Position(4, 0), new Position(4, 4));
        engine.requestMove(new Position(4, 7), new Position(4, 4));

        assertDoesNotThrow(() -> engine.waitMs(4000));
        assertTrue(board.pieceAt(new Position(4, 4)).isPresent());
    }

    @Test
    public void testJumpingPieceDefeatingAttackerDoesNotCrash() {
        Board board = new Board(8, 8);
        Piece jumper = new Piece("wr", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(4, 4));
        Piece attacker = new Piece("br", Piece.Color.BLACK, Piece.Kind.ROOK, new Position(3, 4));
        board.addPiece(jumper, new Position(4, 4));
        board.addPiece(attacker, new Position(3, 4));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        engine.requestJump(new Position(4, 4));
        MoveResult attackResult = engine.requestMove(new Position(3, 4), new Position(4, 4));
        assertTrue(attackResult.isAccepted());

        assertDoesNotThrow(() -> engine.waitMs(1000));
        assertTrue(board.pieceAt(new Position(4, 4)).isPresent());
        assertEquals(jumper, board.pieceAt(new Position(4, 4)).get());
    }

    @Test
    public void testMultiCellMoveDoesNotSnapToDestinationEarly() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        GameEngine engine = new GameEngine(board, new GameState(), ruleEngine(), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 0), new Position(4, 0)); 
        engine.waitMs(500); 

        PieceSnapshot snapshot = engine.snapshot(null).pieceAt(new Position(7, 0));
        assertNotNull(snapshot);
        double progress = 1.0 / 3.0;
        double eased = progress < 0.5 ? 2 * progress * progress : 1 - Math.pow(-2 * progress + 2, 2) / 2;
        int expectedPartialPixelY = (int) Math.round((7 + (4 - 7) * eased) * GameSnapshot.CELL_HEIGHT);
        int destinationPixelY = (int) Math.round(4 * GameSnapshot.CELL_HEIGHT);
        assertEquals(expectedPartialPixelY, snapshot.pixelY());
        assertNotEquals(destinationPixelY, snapshot.pixelY(),
                "one third into a 3-cell move the piece must not already be rendered at the destination");

        engine.waitMs(1000); 
        assertTrue(board.pieceAt(new Position(4, 0)).isPresent());
    }
}
