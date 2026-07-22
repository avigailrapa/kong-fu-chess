package realtime;
import src.model.*;
import src.realtime.*;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RealTimeArbiterTest {

    @Test
    public void testArbiterIsIdleInitially() {
        Board board = new Board(8, 8);
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        assertTrue(arbiter.isIdle());
    }

    @Test
    public void testStartMotionSetsActiveMotionAndMovingState() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(rook, new Position(7, 0), new Position(4, 0));

        assertTrue(arbiter.isMoving(rook));
        assertEquals(Piece.State.MOVING, rook.state());
        assertEquals(new Position(7, 0), rook.cell());
    }

    @Test
    public void testStartMotionForSameMovingPieceThrows() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(4, 0));

        assertThrows(IllegalStateException.class,
                () -> arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0)));
    }

    @Test
    public void testTwoDifferentPiecesCanMoveSimultaneously() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece other = new Piece("r2", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 7));
        board.addPiece(other, new Position(7, 7));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(4, 0));

        arbiter.startMotion(other, new Position(7, 7), new Position(4, 7));

        assertTrue(arbiter.isMoving(rook));
        assertTrue(arbiter.isMoving(other));
    }

    @Test
    public void testBothSimultaneousMotionsArriveInSameTick() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece other = new Piece("r2", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 7));
        board.addPiece(other, new Position(7, 7));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));
        arbiter.startMotion(other, new Position(7, 7), new Position(6, 7));

        List<ArrivalEvent> events = arbiter.advanceTime(1000);

        assertEquals(2, events.size());
        assertFalse(arbiter.isMoving(rook));
        assertFalse(arbiter.isMoving(other));
        assertTrue(board.getPieceAt(new Position(6, 0)).isPresent());
        assertTrue(board.getPieceAt(new Position(6, 7)).isPresent());
    }

    @Test
    public void testAfter999MsPieceHasNotArrived() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));

        List<ArrivalEvent> events = arbiter.advanceTime(499);

        assertTrue(events.isEmpty());
        assertTrue(board.getPieceAt(new Position(7, 0)).isPresent());
        assertTrue(arbiter.isMoving(rook));
    }

    @Test
    public void testAfter1000MsOneSquareMoveArrives() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));

        List<ArrivalEvent> events = arbiter.advanceTime(1000);

        assertFalse(events.isEmpty());
        assertTrue(board.getPieceAt(new Position(7, 0)).isEmpty());
        assertTrue(board.getPieceAt(new Position(6, 0)).isPresent());
        assertEquals(Piece.State.LONG_REST, rook.state());
        assertTrue(arbiter.isLongResting(rook));
        assertFalse(arbiter.isMoving(rook));
    }

    @Test
    public void testPartialWaitFollowedByRemainingWaitEqualsOneFullWait() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(4, 0)); 

        assertTrue(arbiter.advanceTime(1200).isEmpty());
        assertTrue(board.getPieceAt(new Position(7, 0)).isPresent());

        List<ArrivalEvent> events = arbiter.advanceTime(1800);

        assertFalse(events.isEmpty());
        assertTrue(board.getPieceAt(new Position(4, 0)).isPresent());
    }

    @Test
    public void testMultipleSmallWaitsAccumulateCorrectly() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(4, 0));

        for (int i = 0; i < 14; i++) {
            assertTrue(arbiter.advanceTime(100).isEmpty());
        }
        assertTrue(board.getPieceAt(new Position(7, 0)).isPresent());

        assertFalse(arbiter.advanceTime(100).isEmpty());  
        assertTrue(board.getPieceAt(new Position(4, 0)).isPresent());
    }

    @Test
    public void testElapsedTimeResetsForEachNewMotion() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0)); 
        arbiter.advanceTime(500);

        arbiter.startMotion(rook, new Position(6, 0), new Position(4, 0));
        assertTrue(arbiter.advanceTime(500).isEmpty()); 
        assertTrue(board.getPieceAt(new Position(6, 0)).isPresent());

        assertFalse(arbiter.advanceTime(500).isEmpty());
        assertTrue(board.getPieceAt(new Position(4, 0)).isPresent());
    }

    @Test
    public void testAdvanceTimeWithNoActiveMotionIsNoOp() {
        Board board = new Board(8, 8);
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        List<ArrivalEvent> events = arbiter.advanceTime(500);

        assertTrue(events.isEmpty());
        assertTrue(arbiter.isIdle());
    }

    @Test
    public void testArrivalRemovesCapturedEnemyPieceAndMarksItCaptured() {
        Board board = new Board(8, 8);
        Piece bishop = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(7, 0));
        board.addPiece(bishop, new Position(7, 0));
        Piece enemyPawn = new Piece("p1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(4, 3));
        board.addPiece(enemyPawn, new Position(4, 3));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(bishop, new Position(7, 0), new Position(4, 3)); 

        List<ArrivalEvent> events = arbiter.advanceTime(3000);

        assertEquals(1, events.size());
        ArrivalEvent event = events.get(0);
        assertEquals(enemyPawn, event.capturedPiece());
        assertFalse(event.kingCaptured());
        assertEquals(Piece.State.CAPTURED, enemyPawn.state());
        assertTrue(board.getPieceAt(new Position(4, 3)).map(p -> p.id().equals("b1")).orElse(false));
    }

    @Test
    public void testArrivalWithoutCaptureReportsNullCapturedPiece() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));

        ArrivalEvent event = arbiter.advanceTime(1000).get(0);

        assertNull(event.capturedPiece());
        assertFalse(event.kingCaptured());
    }

    @Test
    public void testArrivalCapturingKingReportsKingCapturedTrue() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece king = new Piece("k1", Piece.Color.BLACK, Piece.Kind.KING, new Position(4, 0));
        board.addPiece(king, new Position(4, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(4, 0));

        ArrivalEvent event = arbiter.advanceTime(3000).get(0);

        assertTrue(event.kingCaptured());
    }

    @Test
    public void testArrivalResolutionIsAtomicNoDuplicateEventOnRepeatedAdvance() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));

        assertFalse(arbiter.advanceTime(1000).isEmpty());
        assertTrue(arbiter.advanceTime(1000).isEmpty());
        assertFalse(arbiter.isMoving(rook));
    }

    @Test
    public void testDiagonalMovementUsesCellStepDurationNotEuclideanDistance() {
        Board board = new Board(8, 8);
        Piece bishop = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(0, 0));
        board.addPiece(bishop, new Position(0, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(bishop, new Position(0, 0), new Position(1, 1)); 

        List<ArrivalEvent> events = arbiter.advanceTime(1000);

        assertFalse(events.isEmpty());
        assertTrue(board.getPieceAt(new Position(1, 1)).isPresent());
    }

    @Test
    public void testWhitePawnPromotesToQueenOnArrival() {
        Board board = new Board(3, 2);
        Piece pawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(1, 1));
        board.addPiece(pawn, new Position(1, 1));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(pawn, new Position(1, 1), new Position(0, 1));

        ArrivalEvent event = arbiter.advanceTime(1000).get(0);

        Piece atDestination = board.getPieceAt(new Position(0, 1)).orElseThrow();
        assertEquals(Piece.Kind.QUEEN, atDestination.kind());
        assertEquals(Piece.Color.WHITE, atDestination.color());
        assertEquals(Piece.Kind.QUEEN, event.movedPiece().kind());
        assertEquals("p1", atDestination.id());
        assertTrue(event.promoted());
    }

    @Test
    public void testBlackPawnPromotesToQueenOnArrival() {
        Board board = new Board(3, 2);
        Piece pawn = new Piece("p1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(0, 1));
        board.addPiece(pawn, new Position(0, 1));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(pawn, new Position(0, 1), new Position(1, 1));

        arbiter.advanceTime(1000);

        Piece atDestination = board.getPieceAt(new Position(1, 1)).orElseThrow();
        assertEquals(Piece.Kind.QUEEN, atDestination.kind());
        assertEquals(Piece.Color.BLACK, atDestination.color());
    }

    @Test
    public void testPromotedQueenCanMoveDiagonallyAfterward() {
        Board board = new Board(3, 3);
        Piece pawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(1, 0));
        board.addPiece(pawn, new Position(1, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(pawn, new Position(1, 0), new Position(0, 0));
        arbiter.advanceTime(1000);

        Piece promoted = board.getPieceAt(new Position(0, 0)).orElseThrow();
        assertEquals(Piece.Kind.QUEEN, promoted.kind());

        arbiter.startMotion(promoted, new Position(0, 0), new Position(2, 2));
        List<ArrivalEvent> events = arbiter.advanceTime(2000);

        assertFalse(events.isEmpty());
        assertTrue(board.getPieceAt(new Position(2, 2)).isPresent());
    }

    @Test
    public void testPawnNotOnLastRankDoesNotPromote() {
        Board board = new Board(3, 3);
        Piece pawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(2, 0));
        board.addPiece(pawn, new Position(2, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(pawn, new Position(2, 0), new Position(1, 0));

        ArrivalEvent event = arbiter.advanceTime(1000).get(0);

        Piece atDestination = board.getPieceAt(new Position(1, 0)).orElseThrow();
        assertEquals(Piece.Kind.PAWN, atDestination.kind());
        assertFalse(event.promoted());
    }

    @Test
    public void testThreePiecesCanMoveSimultaneously() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece bishop = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(7, 2));
        board.addPiece(bishop, new Position(7, 2));
        Piece knight = new Piece("n1", Piece.Color.BLACK, Piece.Kind.KNIGHT, new Position(0, 1));
        board.addPiece(knight, new Position(0, 1));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));
        arbiter.startMotion(bishop, new Position(7, 2), new Position(6, 1));
        arbiter.startMotion(knight, new Position(0, 1), new Position(2, 2));

        assertTrue(arbiter.isMoving(rook));
        assertTrue(arbiter.isMoving(bishop));
        assertTrue(arbiter.isMoving(knight));

        List<ArrivalEvent> events = arbiter.advanceTime(500);

        assertEquals(2, events.size());
        assertFalse(arbiter.isMoving(rook));
        assertFalse(arbiter.isMoving(bishop));
        assertTrue(arbiter.isMoving(knight));
        assertTrue(board.getPieceAt(new Position(6, 0)).isPresent());
        assertTrue(board.getPieceAt(new Position(6, 1)).isPresent());
    }

    @Test
    public void testShorterSimultaneousMotionArrivesWhileLongerOneContinues() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece bishop = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(7, 7));
        board.addPiece(bishop, new Position(7, 7));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0)); 
        arbiter.startMotion(bishop, new Position(7, 7), new Position(4, 4)); 

        List<ArrivalEvent> events = arbiter.advanceTime(1000);

        assertEquals(1, events.size());
        assertEquals(rook, events.get(0).movedPiece());
        assertFalse(arbiter.isMoving(rook));
        assertTrue(arbiter.isMoving(bishop));
    }

    @Test
    public void testArrivalEventOrderMatchesMotionStartOrderWhenBothResolveTogether() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece bishop = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(7, 2));
        board.addPiece(bishop, new Position(7, 2));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));
        arbiter.startMotion(bishop, new Position(7, 2), new Position(6, 1));

        List<ArrivalEvent> events = arbiter.advanceTime(1000);

        assertEquals(2, events.size());
        assertEquals(rook, events.get(0).movedPiece());
        assertEquals(bishop, events.get(1).movedPiece());
    }

    @Test
    public void testEnemyMotionsSwappingCellsResolveInFavorOfWhicheverStartedFirst() {
        Board board = new Board(4, 1);
        Piece whiteRook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));
        board.addPiece(whiteRook, new Position(0, 0));
        Piece blackRook = new Piece("r2", Piece.Color.BLACK, Piece.Kind.ROOK, new Position(0, 3));
        board.addPiece(blackRook, new Position(0, 3));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(whiteRook, new Position(0, 0), new Position(0, 3)); 
        arbiter.startMotion(blackRook, new Position(0, 3), new Position(0, 0)); 

        List<ArrivalEvent> events = arbiter.advanceTime(3000);

        assertEquals(1, events.size());
        assertEquals(whiteRook, events.get(0).movedPiece());
        assertEquals(Piece.State.CAPTURED, blackRook.state());
        assertTrue(board.getPieceAt(new Position(0, 3)).map(p -> p.id().equals("r1")).orElse(false));
        assertTrue(board.getPieceAt(new Position(0, 0)).isEmpty());
    }

    @Test
    public void testEnemyMotionsSwappingCellsWhenSecondPieceStartedFirstInsteadWins() {
        Board board = new Board(4, 1);
        Piece whiteRook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));
        board.addPiece(whiteRook, new Position(0, 0));
        Piece blackRook = new Piece("r2", Piece.Color.BLACK, Piece.Kind.ROOK, new Position(0, 3));
        board.addPiece(blackRook, new Position(0, 3));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(blackRook, new Position(0, 3), new Position(0, 0)); 
        arbiter.startMotion(whiteRook, new Position(0, 0), new Position(0, 3));
        List<ArrivalEvent> events = arbiter.advanceTime(3000);

        assertEquals(1, events.size());
        assertEquals(blackRook, events.get(0).movedPiece());
        assertEquals(Piece.State.CAPTURED, whiteRook.state());
        assertTrue(board.getPieceAt(new Position(0, 0)).map(p -> p.id().equals("r2")).orElse(false));
        assertTrue(board.getPieceAt(new Position(0, 3)).isEmpty());
    }

    @Test
    public void testJumpAndMotionOfDifferentPiecesResolveIndependently() {
        Board board = new Board(8, 8);
        Piece king = new Piece("k1", Piece.Color.WHITE, Piece.Kind.KING, new Position(7, 0));
        board.addPiece(king, new Position(7, 0));
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 7));
        board.addPiece(rook, new Position(7, 7));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        arbiter.startJump(king, new Position(7, 0));
        arbiter.startMotion(rook, new Position(7, 7), new Position(6, 7));

        List<ArrivalEvent> events = arbiter.advanceTime(1000);

        
        assertEquals(1, events.size());
        assertEquals(rook, events.get(0).movedPiece());
        assertFalse(arbiter.isJumping(king));
        assertFalse(arbiter.isMoving(rook));
        assertEquals(Piece.State.SHORT_REST, king.state());
        assertTrue(arbiter.isShortResting(king));
        assertTrue(board.getPieceAt(new Position(6, 7)).isPresent());
    }

    @Test
    public void testJumperCapturesPieceThatEnteredItsCellMidFlight() {
        Board board = new Board(8, 8);
        Piece king = new Piece("k1", Piece.Color.WHITE, Piece.Kind.KING, new Position(7, 0));
        board.addPiece(king, new Position(7, 0));
        Piece enemyRook = new Piece("r1", Piece.Color.BLACK, Piece.Kind.ROOK, new Position(7, 1));
        board.addPiece(enemyRook, new Position(7, 1));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

       
        arbiter.startMotion(enemyRook, new Position(7, 1), new Position(7, 0)); 
        arbiter.advanceTime(500);
        arbiter.startJump(king, new Position(7, 0)); 
        assertEquals(Piece.State.JUMPING, king.state());
        arbiter.advanceTime(500); 

        assertTrue(board.getPieceAt(new Position(7, 0)).map(p -> p.id().equals("r1")).orElse(false));
        assertTrue(arbiter.isJumping(king));

        List<ArrivalEvent> events = arbiter.advanceTime(500);

        assertEquals(1, events.size());
        assertFalse(events.get(0).kingCaptured());
        assertEquals(Piece.State.CAPTURED, enemyRook.state());
        assertEquals(Piece.State.SHORT_REST, king.state());
        assertTrue(arbiter.isShortResting(king));
        assertTrue(board.getPieceAt(new Position(7, 0)).map(p -> p.id().equals("k1")).orElse(false));
    }

    @Test
    public void testKingEliminatedByCounterJumpReportsKingCaptured() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece enemyKing = new Piece("k1", Piece.Color.BLACK, Piece.Kind.KING, new Position(6, 0));
        board.addPiece(enemyKing, new Position(6, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startJump(rook, new Position(7, 0));
        arbiter.startMotion(enemyKing, new Position(6, 0), new Position(7, 0));

        List<ArrivalEvent> events = arbiter.advanceTime(1000);

        assertEquals(1, events.size());
        assertTrue(events.get(0).kingCaptured());
        assertEquals(Piece.State.CAPTURED, enemyKing.state());
        assertEquals(Piece.State.SHORT_REST, rook.state());
        assertTrue(arbiter.isShortResting(rook));
    }

    @Test
    public void testJumpingPieceCannotJumpAgainUntilShortRestElapses() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startJump(rook, new Position(7, 0));
        arbiter.advanceTime(1000);
        assertFalse(arbiter.isJumping(rook));
        assertEquals(Piece.State.SHORT_REST, rook.state());
        assertTrue(arbiter.isShortResting(rook));

        arbiter.startJump(rook, new Position(7, 0));
        assertFalse(arbiter.isJumping(rook));

        arbiter.advanceTime(500); 
        assertFalse(arbiter.isShortResting(rook));

        arbiter.startJump(rook, new Position(7, 0));

        assertTrue(arbiter.isJumping(rook));
    }

    @Test
    public void testMotionCannotStartAgainUntilLongRestElapsesButJumpIsStillBlockedToo() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));
        arbiter.advanceTime(1000); 
        assertEquals(Piece.State.LONG_REST, rook.state());
        assertTrue(arbiter.isLongResting(rook));

        arbiter.startJump(rook, new Position(6, 0));
        assertFalse(arbiter.isJumping(rook));

        arbiter.advanceTime(2000); 
        assertFalse(arbiter.isLongResting(rook));

        arbiter.startJump(rook, new Position(6, 0));

        assertTrue(arbiter.isJumping(rook));
    }

    @Test
    public void testTwoDifferentPiecesCanJumpSimultaneously() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece bishop = new Piece("b1", Piece.Color.BLACK, Piece.Kind.BISHOP, new Position(0, 0));
        board.addPiece(bishop, new Position(0, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        arbiter.startJump(rook, new Position(7, 0));
        arbiter.startJump(bishop, new Position(0, 0));

        assertTrue(arbiter.isJumping(rook));
        assertTrue(arbiter.isJumping(bishop));
        assertEquals(Piece.State.JUMPING, rook.state());
        assertEquals(Piece.State.JUMPING, bishop.state());

        List<ArrivalEvent> events = arbiter.advanceTime(1000);

        assertTrue(events.isEmpty());
        assertFalse(arbiter.isJumping(rook));
        assertFalse(arbiter.isJumping(bishop));
        assertEquals(Piece.State.SHORT_REST, rook.state());
        assertEquals(Piece.State.SHORT_REST, bishop.state());
        assertTrue(arbiter.isShortResting(rook));
        assertTrue(arbiter.isShortResting(bishop));
    }

    @Test
    public void testOneOfTwoSimultaneousJumpsCanBeCounteredWhileTheOtherLandsSafely() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece enemyPawn = new Piece("p1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(6, 0));
        board.addPiece(enemyPawn, new Position(6, 0));
        Piece bishop = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(0, 0));
        board.addPiece(bishop, new Position(0, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        arbiter.startJump(rook, new Position(7, 0));
        arbiter.startJump(bishop, new Position(0, 0)); 
        arbiter.startMotion(enemyPawn, new Position(6, 0), new Position(7, 0));

        List<ArrivalEvent> events = arbiter.advanceTime(1000);

        assertEquals(1, events.size());
        assertEquals(rook, events.get(0).movedPiece());
        assertEquals(Piece.State.CAPTURED, enemyPawn.state());
        assertEquals(Piece.State.SHORT_REST, rook.state());
        assertEquals(Piece.State.SHORT_REST, bishop.state());
        assertTrue(arbiter.isShortResting(rook));
        assertTrue(arbiter.isShortResting(bishop));
        assertFalse(arbiter.isJumping(rook));
        assertFalse(arbiter.isJumping(bishop));
    }

    @Test
    public void testFriendlyPieceArrivingLaterBouncesBackInsteadOfCapturingAlly() {
        Board board = new Board(8, 8);
        Piece rookA = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rookA, new Position(7, 0));
        Piece bishopB = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(4, 2));
        board.addPiece(bishopB, new Position(4, 2));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rookA, new Position(7, 0), new Position(6, 0)); 
        arbiter.startMotion(bishopB, new Position(4, 2), new Position(6, 0));

        arbiter.advanceTime(500); 
        assertTrue(board.getPieceAt(new Position(6, 0)).map(p -> p.id().equals("r1")).orElse(false));

        List<ArrivalEvent> events = arbiter.advanceTime(500);

        assertEquals(1, events.size());
        ArrivalEvent event = events.get(0);
        assertEquals(bishopB, event.movedPiece());
        assertEquals(new Position(4, 2), event.from());
        assertEquals(new Position(5, 1), event.to()); 
        assertNull(event.capturedPiece());
        assertEquals(Piece.State.LONG_REST, bishopB.state());
        assertEquals(Piece.State.LONG_REST, rookA.state());
        assertTrue(arbiter.isLongResting(rookA));
        assertTrue(arbiter.isLongResting(bishopB));
        assertTrue(board.getPieceAt(new Position(5, 1)).map(p -> p.id().equals("b1")).orElse(false));
        assertTrue(board.getPieceAt(new Position(6, 0)).map(p -> p.id().equals("r1")).orElse(false));
    }

    @Test
    public void testEnemyRaceWinnerDeterminedByOvershootCapturesLoser() {
        Board board = new Board(8, 8);
        Piece whiteRook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(whiteRook, new Position(7, 0)); 
        Piece blackRook = new Piece("r2", Piece.Color.BLACK, Piece.Kind.ROOK, new Position(4, 0));
        board.addPiece(blackRook, new Position(4, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(whiteRook, new Position(7, 0), new Position(6, 0));
        arbiter.startMotion(blackRook, new Position(4, 0), new Position(6, 0));

        List<ArrivalEvent> events = arbiter.advanceTime(2000);

        assertEquals(2, events.size());
        assertEquals(Piece.State.CAPTURED, whiteRook.state());
        assertEquals(Piece.State.LONG_REST, blackRook.state());
        assertTrue(arbiter.isLongResting(blackRook));
        assertTrue(board.getPieceAt(new Position(6, 0)).map(p -> p.id().equals("r2")).orElse(false));
        assertTrue(board.getPieceAt(new Position(7, 0)).isEmpty());
        assertFalse(arbiter.isMoving(whiteRook));
        assertFalse(arbiter.isMoving(blackRook));
    }

    @Test
    public void testRaceLoserNotCapturedWhenWinnerBouncesBackOffThirdPieceAtDestination() {
        Board board = new Board(8, 8);
        Piece whiteC = new Piece("c", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(whiteC, new Position(7, 0)); 
        Piece whiteA = new Piece("a", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(5, 3));
        board.addPiece(whiteA, new Position(5, 3)); 
        Piece blackB = new Piece("b", Piece.Color.BLACK, Piece.Kind.ROOK, new Position(3, 0));
        board.addPiece(blackB, new Position(3, 0)); 
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(whiteC, new Position(7, 0), new Position(6, 0));
        arbiter.startMotion(whiteA, new Position(5, 3), new Position(6, 0));
        arbiter.startMotion(blackB, new Position(3, 0), new Position(6, 0));

        List<ArrivalEvent> settling = arbiter.advanceTime(500);
        assertEquals(1, settling.size());
        assertTrue(board.getPieceAt(new Position(6, 0)).map(p -> p.id().equals("c")).orElse(false));

        List<ArrivalEvent> raceEvents = arbiter.advanceTime(1000);

        assertEquals(2, raceEvents.size());
        assertEquals(Piece.State.IDLE, whiteA.state());
        assertEquals(Piece.State.IDLE, blackB.state());
        assertTrue(board.getPieceAt(new Position(5, 3)).map(p -> p.id().equals("a")).orElse(false));
        assertTrue(board.getPieceAt(new Position(5, 0)).map(p -> p.id().equals("b")).orElse(false));
        assertTrue(board.getPieceAt(new Position(6, 0)).map(p -> p.id().equals("c")).orElse(false));
    }

    @Test
    public void testFriendlyRaceWinnerDeterminedByOvershootBouncesLoserBack() {
        Board board = new Board(8, 8);
        Piece rookA = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rookA, new Position(7, 0)); 
        Piece rookB = new Piece("r2", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(4, 0));
        board.addPiece(rookB, new Position(4, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rookA, new Position(7, 0), new Position(6, 0));
        arbiter.startMotion(rookB, new Position(4, 0), new Position(6, 0));

        List<ArrivalEvent> events = arbiter.advanceTime(2000);

        assertEquals(2, events.size());
        assertEquals(Piece.State.IDLE, rookA.state());
        assertEquals(Piece.State.LONG_REST, rookB.state());
        assertTrue(arbiter.isLongResting(rookB));
        assertTrue(board.getPieceAt(new Position(6, 0)).map(p -> p.id().equals("r2")).orElse(false));
        assertTrue(board.getPieceAt(new Position(7, 0)).map(p -> p.id().equals("r1")).orElse(false));
    }

    @Test
    public void testSameColorSlidingPathsCrossingStopsTheLaterArrivalBeforeTheCrossingCell() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 4));
        board.addPiece(rook, new Position(7, 4));
        Piece queen = new Piece("q1", Piece.Color.WHITE, Piece.Kind.QUEEN, new Position(4, 0)); 
        board.addPiece(queen, new Position(4, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(queen, new Position(4, 0), new Position(4, 7)); 
        arbiter.advanceTime(700);

        arbiter.startMotion(rook, new Position(7, 4), new Position(0, 4)); 
        arbiter.advanceTime(1);

        Motion rookMotion = arbiter.activeMotion(rook).orElseThrow();
        assertEquals(new Position(5, 4), rookMotion.destination()); 
        assertEquals(1000L, rookMotion.durationMs());
        Motion queenMotion = arbiter.activeMotion(queen).orElseThrow();
        assertEquals(new Position(4, 7), queenMotion.destination()); 

        List<ArrivalEvent> events = arbiter.advanceTime(1000); 
        assertEquals(1, events.size());
        assertEquals(new Position(5, 4), events.get(0).to());
        assertTrue(board.getPieceAt(new Position(5, 4)).map(p -> p.id().equals("r1")).orElse(false));
        assertEquals(Piece.State.LONG_REST, rook.state());
    }

    @Test
    public void testSameColorSlidingPathsThatCrossGeometricallyButNeverShareTheCellInTimeAreNotTruncated() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 4)); 
        board.addPiece(rook, new Position(7, 4));
        Piece queen = new Piece("q1", Piece.Color.WHITE, Piece.Kind.QUEEN, new Position(4, 0)); 
        board.addPiece(queen, new Position(4, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(rook, new Position(7, 4), new Position(0, 4)); 
        arbiter.advanceTime(300);

        arbiter.startMotion(queen, new Position(4, 0), new Position(4, 7)); 
        arbiter.advanceTime(1);

        Motion rookMotion = arbiter.activeMotion(rook).orElseThrow();
        assertEquals(new Position(0, 4), rookMotion.destination());
        Motion queenMotion = arbiter.activeMotion(queen).orElseThrow();
        assertEquals(new Position(4, 7), queenMotion.destination()); 
    }

    @Test
    public void testSameColorSlidingPathsCrossingIsIgnoredOnceTheFirstPieceAlreadyPassedTheCell() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 4)); 
        board.addPiece(rook, new Position(7, 4));
        Piece queen = new Piece("q1", Piece.Color.WHITE, Piece.Kind.QUEEN, new Position(4, 0)); 
        board.addPiece(queen, new Position(4, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(rook, new Position(7, 4), new Position(0, 4)); 
        arbiter.advanceTime(2000); 

        arbiter.startMotion(queen, new Position(4, 0), new Position(4, 7)); 

        Motion rookMotion = arbiter.activeMotion(rook).orElseThrow();
        assertEquals(new Position(0, 4), rookMotion.destination()); 
        Motion queenMotion = arbiter.activeMotion(queen).orElseThrow();
        assertEquals(new Position(4, 7), queenMotion.destination());
    }

}
