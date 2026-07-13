package realtime;
import src.model.*;
import src.realtime.*;

import org.junit.jupiter.api.Test;

import java.util.Optional;

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
    public void testStartMotionWhileAlreadyActiveThrows() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece other = new Piece("r2", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 7));
        board.addPiece(other, new Position(7, 7));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(4, 0));

        assertThrows(IllegalStateException.class,
                () -> arbiter.startMotion(other, new Position(7, 7), new Position(4, 7)));
    }

    @Test
    public void testAfter999MsPieceHasNotArrived() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));

        Optional<ArrivalEvent> event = arbiter.advanceTime(999);

        assertTrue(event.isEmpty());
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

        Optional<ArrivalEvent> event = arbiter.advanceTime(1000);

        assertTrue(event.isPresent());
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

        Optional<ArrivalEvent> event = arbiter.advanceTime(1800); // 1200 + 1800 = 3000

        assertTrue(event.isPresent());
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

        assertTrue(arbiter.advanceTime(100).isPresent()); // 30th x 100ms = 3000ms total
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

        assertTrue(arbiter.advanceTime(1000).isPresent());
        assertTrue(board.getPieceAt(new Position(4, 0)).isPresent());
    }

    @Test
    public void testAdvanceTimeWithNoActiveMotionIsNoOp() {
        Board board = new Board(8, 8);
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        Optional<ArrivalEvent> event = arbiter.advanceTime(500);

        assertTrue(event.isEmpty());
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

        Optional<ArrivalEvent> event = arbiter.advanceTime(3000);

        assertTrue(event.isPresent());
        assertEquals(enemyPawn, event.get().capturedPiece());
        assertFalse(event.get().kingCaptured());
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

        ArrivalEvent event = arbiter.advanceTime(1000).orElseThrow();

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

        ArrivalEvent event = arbiter.advanceTime(3000).orElseThrow();

        assertTrue(event.kingCaptured());
    }

    @Test
    public void testArrivalResolutionIsAtomicNoDuplicateEventOnRepeatedAdvance() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));

        assertTrue(arbiter.advanceTime(1000).isPresent());
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

        Optional<ArrivalEvent> event = arbiter.advanceTime(1000);

        assertTrue(event.isPresent());
        assertTrue(board.getPieceAt(new Position(1, 1)).isPresent());
    }

    @Test
    public void testWhitePawnPromotesToQueenOnArrival() {
        Board board = new Board(3, 2);
        Piece pawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(1, 1));
        board.addPiece(pawn, new Position(1, 1));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(pawn, new Position(1, 1), new Position(0, 1));

        ArrivalEvent event = arbiter.advanceTime(1000).orElseThrow();

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
        Optional<ArrivalEvent> event = arbiter.advanceTime(2000);

        assertTrue(event.isPresent());
        assertTrue(board.getPieceAt(new Position(2, 2)).isPresent());
    }

    @Test
    public void testPieceNotOnCooldownBeforeAnyMotion() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);

        assertFalse(arbiter.isOnCooldown(rook));
    }

    @Test
    public void testPieceOnCooldownImmediatelyAfterArrival() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));

        arbiter.advanceTime(1000);

        assertTrue(arbiter.isOnCooldown(rook));
    }

    @Test
    public void testCooldownExpiresAfterConfiguredDuration() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));
        arbiter.advanceTime(1000);
        assertTrue(arbiter.isOnCooldown(rook));

        arbiter.advanceTime(1000);

        assertFalse(arbiter.isOnCooldown(rook));
    }

    @Test
    public void testCooldownDoesNotAffectOtherPieces() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        Piece bishop = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(7, 2));
        board.addPiece(bishop, new Position(7, 2));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));

        arbiter.advanceTime(1000);

        assertTrue(arbiter.isOnCooldown(rook));
        assertFalse(arbiter.isOnCooldown(bishop));
    }

    @Test
    public void testCapturedPieceStillEntersCooldownState() {
        Board board = new Board(8, 8);
        Piece bishop = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(7, 0));
        board.addPiece(bishop, new Position(7, 0));
        Piece enemyPawn = new Piece("p1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(4, 3));
        board.addPiece(enemyPawn, new Position(4, 3));
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        arbiter.startMotion(bishop, new Position(7, 0), new Position(4, 3));

        arbiter.advanceTime(3000);

        assertTrue(arbiter.isOnCooldown(bishop));
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
}
