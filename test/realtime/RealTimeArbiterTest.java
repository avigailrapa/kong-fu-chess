package realtime;
import src.model.*;
import src.realtime.*;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RealTimeArbiterTest {

    @Test
    public void testHasActiveMotionFalseInitially() {
        Board board = new Board(8, 8);
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        assertFalse(arbiter.hasActiveMotion());
    }

    @Test
    public void testStartMotionSetsActiveMotionAndMovingState() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(rook, new Position(7, 0), new Position(4, 0));

        assertTrue(arbiter.hasActiveMotion());
        assertEquals(Piece.State.MOVING, rook.getState());
        assertEquals(new Position(7, 0), rook.getCell());
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

        List<ArrivalEvent> events = arbiter.advanceTime(999);

        assertTrue(events.isEmpty());
        assertTrue(board.getPieceAt(new Position(7, 0)).isPresent());
        assertTrue(arbiter.hasActiveMotion());
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
        assertEquals(Piece.State.IDLE, rook.getState());
        assertFalse(arbiter.hasActiveMotion());
    }

    @Test
    public void testPartialWaitFollowedByRemainingWaitEqualsOneFullWait() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(4, 0)); // 3 squares = 3000ms

        assertTrue(arbiter.advanceTime(1200).isEmpty());
        assertTrue(board.getPieceAt(new Position(7, 0)).isPresent());

        List<ArrivalEvent> events = arbiter.advanceTime(1800); // 1200 + 1800 = 3000

        assertFalse(events.isEmpty());
        assertTrue(board.getPieceAt(new Position(4, 0)).isPresent());
    }

    @Test
    public void testMultipleSmallWaitsAccumulateCorrectly() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(4, 0)); // 3000ms needed

        for (int i = 0; i < 29; i++) {
            assertTrue(arbiter.advanceTime(100).isEmpty());
        }
        assertTrue(board.getPieceAt(new Position(7, 0)).isPresent());

        assertFalse(arbiter.advanceTime(100).isEmpty()); // 30th x 100ms = 3000ms total
        assertTrue(board.getPieceAt(new Position(4, 0)).isPresent());
    }

    @Test
    public void testElapsedTimeResetsForEachNewMotion() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0)); // 1000ms
        arbiter.advanceTime(1000);

        arbiter.startMotion(rook, new Position(6, 0), new Position(4, 0)); // 2000ms
        assertTrue(arbiter.advanceTime(1000).isEmpty()); // only half of the new motion's duration
        assertTrue(board.getPieceAt(new Position(6, 0)).isPresent());

        assertFalse(arbiter.advanceTime(1000).isEmpty());
        assertTrue(board.getPieceAt(new Position(4, 0)).isPresent());
    }

    @Test
    public void testAdvanceTimeWithNoActiveMotionIsNoOp() {
        Board board = new Board(8, 8);
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        List<ArrivalEvent> events = arbiter.advanceTime(500);

        assertTrue(events.isEmpty());
        assertFalse(arbiter.hasActiveMotion());
    }

    @Test
    public void testArrivalRemovesCapturedEnemyPieceAndMarksItCaptured() {
        Board board = new Board(8, 8);
        Piece bishop = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(7, 0));
        board.addPiece(bishop, new Position(7, 0));
        Piece enemyPawn = new Piece("p1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(4, 3));
        board.addPiece(enemyPawn, new Position(4, 3));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(bishop, new Position(7, 0), new Position(4, 3)); // diagonal, 3 squares

        List<ArrivalEvent> events = arbiter.advanceTime(3000);

        assertEquals(1, events.size());
        ArrivalEvent event = events.get(0);
        assertEquals(enemyPawn, event.capturedPiece());
        assertFalse(event.kingCaptured());
        assertEquals(Piece.State.CAPTURED, enemyPawn.getState());
        assertTrue(board.getPieceAt(new Position(4, 3)).map(p -> p.getId().equals("b1")).orElse(false));
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
        assertFalse(arbiter.hasActiveMotion());
    }

    @Test
    public void testDiagonalMovementUsesCellStepDurationNotEuclideanDistance() {
        Board board = new Board(8, 8);
        Piece bishop = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(0, 0));
        board.addPiece(bishop, new Position(0, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(bishop, new Position(0, 0), new Position(1, 1)); // one diagonal square

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
        assertEquals(Piece.Kind.QUEEN, atDestination.getKind());
        assertEquals(Piece.Color.WHITE, atDestination.getColor());
        assertEquals(Piece.Kind.QUEEN, event.movedPiece().getKind());
        assertEquals("p1", atDestination.getId());
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
        assertEquals(Piece.Kind.QUEEN, atDestination.getKind());
        assertEquals(Piece.Color.BLACK, atDestination.getColor());
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
        assertEquals(Piece.Kind.QUEEN, promoted.getKind());

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

        arbiter.advanceTime(1000);

        Piece atDestination = board.getPieceAt(new Position(1, 0)).orElseThrow();
        assertEquals(Piece.Kind.PAWN, atDestination.getKind());
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

        List<ArrivalEvent> events = arbiter.advanceTime(1000);

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
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0)); // 1000ms
        arbiter.startMotion(bishop, new Position(7, 7), new Position(4, 4)); // 3000ms

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

        arbiter.startMotion(whiteRook, new Position(0, 0), new Position(0, 3)); // started first
        arbiter.startMotion(blackRook, new Position(0, 3), new Position(0, 0)); // started second

        List<ArrivalEvent> events = arbiter.advanceTime(3000);

        assertEquals(1, events.size());
        assertEquals(whiteRook, events.get(0).movedPiece());
        assertEquals(Piece.State.CAPTURED, blackRook.getState());
        assertTrue(board.getPieceAt(new Position(0, 3)).map(p -> p.getId().equals("r1")).orElse(false));
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

        arbiter.startMotion(blackRook, new Position(0, 3), new Position(0, 0)); // started first
        arbiter.startMotion(whiteRook, new Position(0, 0), new Position(0, 3)); // started second

        List<ArrivalEvent> events = arbiter.advanceTime(3000);

        assertEquals(1, events.size());
        assertEquals(blackRook, events.get(0).movedPiece());
        assertEquals(Piece.State.CAPTURED, whiteRook.getState());
        assertTrue(board.getPieceAt(new Position(0, 0)).map(p -> p.getId().equals("r2")).orElse(false));
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

        // The king's jump lands safely (nothing displaced it), which yields no ArrivalEvent;
        // only the rook's motion produces one.
        assertEquals(1, events.size());
        assertEquals(rook, events.get(0).movedPiece());
        assertFalse(arbiter.hasActiveJump());
        assertFalse(arbiter.isMoving(rook));
        assertEquals(Piece.State.IDLE, king.getState());
        assertTrue(board.getPieceAt(new Position(6, 7)).isPresent());
    }

    @Test
    public void testKingCapturedOnJumpLandingReportsKingCaptured() {
        Board board = new Board(8, 8);
        Piece king = new Piece("k1", Piece.Color.WHITE, Piece.Kind.KING, new Position(7, 0));
        board.addPiece(king, new Position(7, 0));
        Piece enemyRook = new Piece("r1", Piece.Color.BLACK, Piece.Kind.ROOK, new Position(7, 1));
        board.addPiece(enemyRook, new Position(7, 1));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        // Enemy rook starts attacking the king's cell first, and is already half-way there
        // when the king starts its jump - so the rook displaces the king mid-air (no capture yet),
        // and only when the king's own jump timer expires does it find the rook already there.
        arbiter.startMotion(enemyRook, new Position(7, 1), new Position(7, 0)); // 1000ms
        arbiter.advanceTime(500);
        arbiter.startJump(king, new Position(7, 0)); // needs its own 1000ms from here
        arbiter.advanceTime(500); // rook's motion (500+500=1000ms) resolves as a silent displacement

        assertTrue(board.getPieceAt(new Position(7, 0)).map(p -> p.getId().equals("r1")).orElse(false));
        assertTrue(arbiter.hasActiveJump());

        List<ArrivalEvent> events = arbiter.advanceTime(500); // king's jump (500+500=1000ms) lands into the occupied cell

        assertEquals(1, events.size());
        assertTrue(events.get(0).kingCaptured());
        assertEquals(Piece.State.CAPTURED, king.getState());
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
        assertEquals(Piece.State.CAPTURED, enemyKing.getState());
        assertEquals(Piece.State.IDLE, rook.getState());
    }

    @Test
    public void testJumpingPieceCanBeCommandedToJumpAgainImmediatelyAfterLandingSafely() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startJump(rook, new Position(7, 0));
        arbiter.advanceTime(1000);
        assertFalse(arbiter.hasActiveJump());

        arbiter.startJump(rook, new Position(7, 0));

        assertTrue(arbiter.hasActiveJump());
    }

    @Test
    public void testJumpCanStartImmediatelyAfterAPreviousMotionArrives() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));
        arbiter.advanceTime(1000); // rook arrives

        arbiter.startJump(rook, new Position(6, 0));

        assertTrue(arbiter.hasActiveJump());
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

        List<ArrivalEvent> events = arbiter.advanceTime(1000);

        // Both land safely (nothing displaced either of them), so no ArrivalEvent for either.
        assertTrue(events.isEmpty());
        assertFalse(arbiter.hasActiveJump());
        assertEquals(Piece.State.IDLE, rook.getState());
        assertEquals(Piece.State.IDLE, bishop.getState());
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
        arbiter.startJump(bishop, new Position(0, 0)); // lands safely, unrelated to the collision below
        arbiter.startMotion(enemyPawn, new Position(6, 0), new Position(7, 0)); // exact tie with rook's jump

        List<ArrivalEvent> events = arbiter.advanceTime(1000);

        assertEquals(1, events.size());
        assertEquals(rook, events.get(0).movedPiece());
        assertEquals(Piece.State.CAPTURED, enemyPawn.getState());
        assertEquals(Piece.State.IDLE, rook.getState());
        assertEquals(Piece.State.IDLE, bishop.getState());
        assertFalse(arbiter.hasActiveJump());
    }
}
