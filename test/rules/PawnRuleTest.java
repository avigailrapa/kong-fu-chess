package rules;
import src.model.*;
import src.rules.*;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PawnRuleTest {

    private final PawnRule pawnRule = new PawnRule();

    @Test
    public void testWhitePawnMovesToLowerRow() {
        Board board = new Board(8, 8);
        Piece pawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(6, 4));
        board.addPiece(pawn, new Position(6, 4));

        Set<Position> destinations = pawnRule.legalDestinations(board, pawn);

        assertTrue(destinations.contains(new Position(5, 4)));
    }

    @Test
    public void testBlackPawnMovesToHigherRow() {
        Board board = new Board(8, 8);
        Piece pawn = new Piece("p1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(1, 4));
        board.addPiece(pawn, new Position(1, 4));

        Set<Position> destinations = pawnRule.legalDestinations(board, pawn);

        assertTrue(destinations.contains(new Position(2, 4)));
    }

    @Test
    public void testNoTwoStepMoveFromNonStartingRow() {
        Board board = new Board(8, 8);
        Piece pawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(6, 4));
        board.addPiece(pawn, new Position(6, 4));

        Set<Position> destinations = pawnRule.legalDestinations(board, pawn);

        assertFalse(destinations.contains(new Position(4, 4)));
    }

    @Test
    public void testWhitePawnTwoStepFromStartingRow() {
        Board board = new Board(8, 8);
        Piece pawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(7, 4));
        board.addPiece(pawn, new Position(7, 4));

        Set<Position> destinations = pawnRule.legalDestinations(board, pawn);

        assertTrue(destinations.contains(new Position(5, 4)));
        assertTrue(destinations.contains(new Position(6, 4)));
    }

    @Test
    public void testBlackPawnTwoStepFromStartingRow() {
        Board board = new Board(8, 8);
        Piece pawn = new Piece("p1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(0, 4));
        board.addPiece(pawn, new Position(0, 4));

        Set<Position> destinations = pawnRule.legalDestinations(board, pawn);

        assertTrue(destinations.contains(new Position(2, 4)));
        assertTrue(destinations.contains(new Position(1, 4)));
    }

    @Test
    public void testTwoStepBlockedByPieceInPath() {
        Board board = new Board(8, 8);
        Piece pawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(7, 4));
        board.addPiece(pawn, new Position(7, 4));
        board.addPiece(new Piece("e1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(6, 4)), new Position(6, 4));

        Set<Position> destinations = pawnRule.legalDestinations(board, pawn);

        assertFalse(destinations.contains(new Position(5, 4)));
        assertFalse(destinations.contains(new Position(6, 4)));
    }

    @Test
    public void testTwoStepBlockedByPieceAtDestinationOnly() {
        Board board = new Board(8, 8);
        Piece pawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(7, 4));
        board.addPiece(pawn, new Position(7, 4));
        board.addPiece(new Piece("e1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(5, 4)), new Position(5, 4));

        Set<Position> destinations = pawnRule.legalDestinations(board, pawn);

        assertTrue(destinations.contains(new Position(6, 4)));
        assertFalse(destinations.contains(new Position(5, 4)));
    }

    @Test
    public void testForwardMoveBlockedByAnyPiece() {
        Board board = new Board(8, 8);
        Piece pawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(6, 4));
        board.addPiece(pawn, new Position(6, 4));
        board.addPiece(new Piece("e1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(5, 4)), new Position(5, 4));

        Set<Position> destinations = pawnRule.legalDestinations(board, pawn);

        assertFalse(destinations.contains(new Position(5, 4)));
    }

    @Test
    public void testCapturesDiagonallyOnly() {
        Board board = new Board(8, 8);
        Piece pawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(6, 4));
        board.addPiece(pawn, new Position(6, 4));
        board.addPiece(new Piece("e1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(5, 3)), new Position(5, 3));
        board.addPiece(new Piece("e2", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(5, 5)), new Position(5, 5));

        Set<Position> destinations = pawnRule.legalDestinations(board, pawn);

        assertTrue(destinations.contains(new Position(5, 3)));
        assertTrue(destinations.contains(new Position(5, 5)));
    }

    @Test
    public void testCannotMoveDiagonallyWithoutCapture() {
        Board board = new Board(8, 8);
        Piece pawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(6, 4));
        board.addPiece(pawn, new Position(6, 4));

        Set<Position> destinations = pawnRule.legalDestinations(board, pawn);

        assertFalse(destinations.contains(new Position(5, 3)));
        assertFalse(destinations.contains(new Position(5, 5)));
    }

    @Test
    public void testCannotCaptureFriendlyDiagonally() {
        Board board = new Board(8, 8);
        Piece pawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(6, 4));
        board.addPiece(pawn, new Position(6, 4));
        board.addPiece(new Piece("f1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(5, 3)), new Position(5, 3));

        Set<Position> destinations = pawnRule.legalDestinations(board, pawn);

        assertFalse(destinations.contains(new Position(5, 3)));
    }

    @Test
    public void testWhitePawnAtBoardEdgeHasNoForwardDestination() {
        Board board = new Board(8, 8);
        Piece pawn = new Piece("p1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(0, 4));
        board.addPiece(pawn, new Position(0, 4));

        Set<Position> destinations = pawnRule.legalDestinations(board, pawn);

        assertTrue(destinations.isEmpty());
    }

    @Test
    public void testBlackPawnAtBoardEdgeHasNoForwardDestination() {
        Board board = new Board(8, 8);
        Piece pawn = new Piece("p1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(7, 4));
        board.addPiece(pawn, new Position(7, 4));

        Set<Position> destinations = pawnRule.legalDestinations(board, pawn);

        assertTrue(destinations.isEmpty());
    }
}
