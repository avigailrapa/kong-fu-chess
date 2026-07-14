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
        assertTrue(arbiter.isLongResting(rook));
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

        // Enemy rook starts attacking the king's cell first, and is already half-way there
        // when the king starts its jump - so the rook displaces the king mid-air (no capture yet),
        // and only when the king's own jump timer expires does it land back and capture the rook
        // that snuck into its cell.
        arbiter.startMotion(enemyRook, new Position(7, 1), new Position(7, 0)); // 1000ms
        arbiter.advanceTime(500);
        arbiter.startJump(king, new Position(7, 0)); // needs its own 1000ms from here
        assertEquals(Piece.State.JUMPING, king.getState());
        arbiter.advanceTime(500); // rook's motion (500+500=1000ms) resolves as a silent displacement

        assertTrue(board.getPieceAt(new Position(7, 0)).map(p -> p.getId().equals("r1")).orElse(false));
        assertTrue(arbiter.hasActiveJump());

        List<ArrivalEvent> events = arbiter.advanceTime(500); // king's jump (500+500=1000ms) lands and eats the rook

        assertEquals(1, events.size());
        assertFalse(events.get(0).kingCaptured());
        assertEquals(Piece.State.CAPTURED, enemyRook.getState());
        assertEquals(Piece.State.IDLE, king.getState());
        assertTrue(arbiter.isShortResting(king));
        assertTrue(board.getPieceAt(new Position(7, 0)).map(p -> p.getId().equals("k1")).orElse(false));
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
        assertFalse(arbiter.hasActiveJump());
        assertEquals(Piece.State.IDLE, rook.getState());
        assertTrue(arbiter.isShortResting(rook));

        arbiter.startJump(rook, new Position(7, 0));
        assertFalse(arbiter.hasActiveJump());

        arbiter.advanceTime(500); // short rest (500ms) elapses
        assertFalse(arbiter.isShortResting(rook));

        arbiter.startJump(rook, new Position(7, 0));

        assertTrue(arbiter.hasActiveJump());
    }

    @Test
    public void testMotionCannotStartAgainUntilLongRestElapsesButJumpIsStillBlockedToo() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));
        arbiter.advanceTime(1000); // rook arrives
        assertEquals(Piece.State.IDLE, rook.getState());
        assertTrue(arbiter.isLongResting(rook));

        arbiter.startJump(rook, new Position(6, 0));
        assertFalse(arbiter.hasActiveJump());

        arbiter.advanceTime(2000); // long rest (2000ms) elapses
        assertFalse(arbiter.isLongResting(rook));

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
        assertEquals(Piece.State.JUMPING, rook.getState());
        assertEquals(Piece.State.JUMPING, bishop.getState());

        List<ArrivalEvent> events = arbiter.advanceTime(1000);

        // Both land safely (nothing displaced either of them), so no ArrivalEvent for either.
        assertTrue(events.isEmpty());
        assertFalse(arbiter.hasActiveJump());
        assertEquals(Piece.State.IDLE, rook.getState());
        assertEquals(Piece.State.IDLE, bishop.getState());
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
        arbiter.startJump(bishop, new Position(0, 0)); // lands safely, unrelated to the collision below
        arbiter.startMotion(enemyPawn, new Position(6, 0), new Position(7, 0)); // exact tie with rook's jump

        List<ArrivalEvent> events = arbiter.advanceTime(1000);

        assertEquals(1, events.size());
        assertEquals(rook, events.get(0).movedPiece());
        assertEquals(Piece.State.CAPTURED, enemyPawn.getState());
        assertEquals(Piece.State.IDLE, rook.getState());
        assertEquals(Piece.State.IDLE, bishop.getState());
        assertTrue(arbiter.isShortResting(rook));
        assertTrue(arbiter.isShortResting(bishop));
        assertFalse(arbiter.hasActiveJump());
    }

    @Test
    public void testFriendlyPieceArrivingLaterBouncesBackInsteadOfCapturingAlly() {
        Board board = new Board(8, 8);
        Piece rookA = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rookA, new Position(7, 0));
        Piece bishopB = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(4, 2));
        board.addPiece(bishopB, new Position(4, 2));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rookA, new Position(7, 0), new Position(6, 0)); // 1000ms
        arbiter.startMotion(bishopB, new Position(4, 2), new Position(6, 0)); // diagonal, 2 squares = 2000ms

        arbiter.advanceTime(1000); // rookA arrives first, cell (6,0) now occupied by a friendly piece
        assertTrue(board.getPieceAt(new Position(6, 0)).map(p -> p.getId().equals("r1")).orElse(false));

        List<ArrivalEvent> events = arbiter.advanceTime(1000); // bishopB's motion is now due too

        assertEquals(1, events.size());
        ArrivalEvent event = events.get(0);
        assertEquals(bishopB, event.movedPiece());
        assertEquals(new Position(4, 2), event.from());
        assertEquals(new Position(4, 2), event.to());
        assertNull(event.capturedPiece());
        assertEquals(Piece.State.IDLE, bishopB.getState());
        assertEquals(Piece.State.IDLE, rookA.getState());
        assertTrue(arbiter.isLongResting(rookA));
        assertTrue(board.getPieceAt(new Position(4, 2)).map(p -> p.getId().equals("b1")).orElse(false));
        assertTrue(board.getPieceAt(new Position(6, 0)).map(p -> p.getId().equals("r1")).orElse(false));
    }

    @Test
    public void testEnemyRaceWinnerDeterminedByOvershootCapturesLoser() {
        Board board = new Board(8, 8);
        Piece whiteRook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(whiteRook, new Position(7, 0)); // 1 square to (6,0) = 1000ms
        Piece blackRook = new Piece("r2", Piece.Color.BLACK, Piece.Kind.ROOK, new Position(4, 0));
        board.addPiece(blackRook, new Position(4, 0)); // 2 squares to (6,0) = 2000ms
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(whiteRook, new Position(7, 0), new Position(6, 0));
        arbiter.startMotion(blackRook, new Position(4, 0), new Position(6, 0));

        // Single tick where both are due: whiteRook overshoots by 1000ms, blackRook by 0ms.
        // blackRook wins (smaller overshoot = arrived later in continuous time).
        List<ArrivalEvent> events = arbiter.advanceTime(2000);

        assertEquals(2, events.size());
        assertEquals(Piece.State.CAPTURED, whiteRook.getState());
        assertEquals(Piece.State.IDLE, blackRook.getState());
        assertTrue(arbiter.isLongResting(blackRook));
        assertTrue(board.getPieceAt(new Position(6, 0)).map(p -> p.getId().equals("r2")).orElse(false));
        assertTrue(board.getPieceAt(new Position(7, 0)).isEmpty());
        assertFalse(arbiter.isMoving(whiteRook));
        assertFalse(arbiter.isMoving(blackRook));
    }

    @Test
    public void testFriendlyRaceWinnerDeterminedByOvershootBouncesLoserBack() {
        Board board = new Board(8, 8);
        Piece rookA = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rookA, new Position(7, 0)); // 1 square to (6,0) = 1000ms
        Piece rookB = new Piece("r2", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(4, 0));
        board.addPiece(rookB, new Position(4, 0)); // 2 squares to (6,0) = 2000ms
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rookA, new Position(7, 0), new Position(6, 0));
        arbiter.startMotion(rookB, new Position(4, 0), new Position(6, 0));

        List<ArrivalEvent> events = arbiter.advanceTime(2000);

        assertEquals(2, events.size());
        assertEquals(Piece.State.IDLE, rookA.getState());
        assertEquals(Piece.State.IDLE, rookB.getState());
        assertTrue(arbiter.isLongResting(rookB));
        assertTrue(board.getPieceAt(new Position(6, 0)).map(p -> p.getId().equals("r2")).orElse(false));
        assertTrue(board.getPieceAt(new Position(7, 0)).map(p -> p.getId().equals("r1")).orElse(false));
    }

}
