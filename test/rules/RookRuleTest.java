package rules;
import src.model.*;
import src.rules.*;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RookRuleTest {

    private final RookRule rookRule = new RookRule();

    @Test
    public void testLegalDestinationsOnEmptyBoard() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(4, 4));
        board.addPiece(rook, new Position(4, 4));

        Set<Position> destinations = rookRule.legalDestinations(board, rook);

        assertEquals(14, destinations.size());
        assertTrue(destinations.contains(new Position(0, 4)));
        assertTrue(destinations.contains(new Position(7, 4)));
        assertTrue(destinations.contains(new Position(4, 0)));
        assertTrue(destinations.contains(new Position(4, 7)));
    }

    @Test
    public void testBlockedByFriendlyPiece() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));
        board.addPiece(rook, new Position(0, 0));
        board.addPiece(new Piece("f1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(0, 3)), new Position(0, 3));

        Set<Position> destinations = rookRule.legalDestinations(board, rook);

        assertTrue(destinations.contains(new Position(0, 1)));
        assertTrue(destinations.contains(new Position(0, 2)));
        assertFalse(destinations.contains(new Position(0, 3)));
        assertFalse(destinations.contains(new Position(0, 4)));
    }

    @Test
    public void testIncludesEnemyBlockerAsLegalDestination() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));
        board.addPiece(rook, new Position(0, 0));
        board.addPiece(new Piece("e1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(0, 3)), new Position(0, 3));

        Set<Position> destinations = rookRule.legalDestinations(board, rook);

        assertTrue(destinations.contains(new Position(0, 3)));
    }

    @Test
    public void testCannotPassEnemyBlocker() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));
        board.addPiece(rook, new Position(0, 0));
        board.addPiece(new Piece("e1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(0, 3)), new Position(0, 3));

        Set<Position> destinations = rookRule.legalDestinations(board, rook);

        assertFalse(destinations.contains(new Position(0, 4)));
        assertFalse(destinations.contains(new Position(0, 5)));
    }

    @Test
    public void testCannotMoveDiagonally() {
        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(4, 4));
        board.addPiece(rook, new Position(4, 4));

        Set<Position> destinations = rookRule.legalDestinations(board, rook);

        assertFalse(destinations.contains(new Position(5, 5)));
        assertFalse(destinations.contains(new Position(3, 3)));
        assertFalse(destinations.contains(new Position(5, 3)));
        assertFalse(destinations.contains(new Position(3, 5)));
    }
}
