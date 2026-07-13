package rules;
import src.model.*;
import src.rules.*;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class KingRuleTest {

    private final KingRule kingRule = new KingRule();

    @Test
    public void testMovesOneCellInEveryDirection() {
        Board board = new Board(8, 8);
        Piece king = new Piece("k1", Piece.Color.WHITE, Piece.Kind.KING, new Position(4, 4));
        board.addPiece(king, new Position(4, 4));

        Set<Position> destinations = kingRule.legalDestinations(board, king);

        assertEquals(8, destinations.size());
        assertTrue(destinations.contains(new Position(3, 4)));
        assertTrue(destinations.contains(new Position(5, 5)));
    }

    @Test
    public void testDoesNotMoveTwoCells() {
        Board board = new Board(8, 8);
        Piece king = new Piece("k1", Piece.Color.WHITE, Piece.Kind.KING, new Position(4, 4));
        board.addPiece(king, new Position(4, 4));

        Set<Position> destinations = kingRule.legalDestinations(board, king);

        assertFalse(destinations.contains(new Position(2, 4)));
        assertFalse(destinations.contains(new Position(4, 6)));
        assertFalse(destinations.contains(new Position(6, 6)));
    }

    @Test
    public void testExcludesFriendlyDestination() {
        Board board = new Board(8, 8);
        Piece king = new Piece("k1", Piece.Color.WHITE, Piece.Kind.KING, new Position(4, 4));
        board.addPiece(king, new Position(4, 4));
        board.addPiece(new Piece("f1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(3, 4)), new Position(3, 4));

        Set<Position> destinations = kingRule.legalDestinations(board, king);

        assertFalse(destinations.contains(new Position(3, 4)));
        assertEquals(7, destinations.size());
    }

    @Test
    public void testIncludesEnemyDestination() {
        Board board = new Board(8, 8);
        Piece king = new Piece("k1", Piece.Color.WHITE, Piece.Kind.KING, new Position(4, 4));
        board.addPiece(king, new Position(4, 4));
        board.addPiece(new Piece("e1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(3, 4)), new Position(3, 4));

        Set<Position> destinations = kingRule.legalDestinations(board, king);

        assertTrue(destinations.contains(new Position(3, 4)));
    }
}
