package rules;
import src.model.*;
import src.rules.*;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class KnightRuleTest {

    private final KnightRule knightRule = new KnightRule();

    @Test
    public void testLShapedJumpsOnEmptyBoard() {
        Board board = new Board(8, 8);
        Piece knight = new Piece("n1", Piece.Color.WHITE, Piece.Kind.KNIGHT, new Position(4, 4));
        board.addPiece(knight, new Position(4, 4));

        Set<Position> destinations = knightRule.legalDestinations(board, knight);

        assertEquals(8, destinations.size());
        assertTrue(destinations.contains(new Position(2, 3)));
        assertTrue(destinations.contains(new Position(6, 5)));
    }

    @Test
    public void testJumpsOverBlockers() {
        Board board = new Board(8, 8);
        Piece knight = new Piece("n1", Piece.Color.WHITE, Piece.Kind.KNIGHT, new Position(4, 4));
        board.addPiece(knight, new Position(4, 4));
        // surround the knight completely with friendly pieces on all adjacent squares
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                Position p = new Position(4 + dr, 4 + dc);
                board.addPiece(new Piece("f" + dr + dc, Piece.Color.WHITE, Piece.Kind.PAWN, p), p);
            }
        }

        Set<Position> destinations = knightRule.legalDestinations(board, knight);

        assertEquals(8, destinations.size());
    }

    @Test
    public void testExcludesFriendlyDestination() {
        Board board = new Board(8, 8);
        Piece knight = new Piece("n1", Piece.Color.WHITE, Piece.Kind.KNIGHT, new Position(4, 4));
        board.addPiece(knight, new Position(4, 4));
        board.addPiece(new Piece("f1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(2, 3)), new Position(2, 3));

        Set<Position> destinations = knightRule.legalDestinations(board, knight);

        assertFalse(destinations.contains(new Position(2, 3)));
        assertEquals(7, destinations.size());
    }

    @Test
    public void testIncludesEnemyDestination() {
        Board board = new Board(8, 8);
        Piece knight = new Piece("n1", Piece.Color.WHITE, Piece.Kind.KNIGHT, new Position(4, 4));
        board.addPiece(knight, new Position(4, 4));
        board.addPiece(new Piece("e1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(2, 3)), new Position(2, 3));

        Set<Position> destinations = knightRule.legalDestinations(board, knight);

        assertTrue(destinations.contains(new Position(2, 3)));
    }

    @Test
    public void testExcludesNonLShapedDestinations() {
        Board board = new Board(8, 8);
        Piece knight = new Piece("n1", Piece.Color.WHITE, Piece.Kind.KNIGHT, new Position(4, 4));
        board.addPiece(knight, new Position(4, 4));

        Set<Position> destinations = knightRule.legalDestinations(board, knight);

        assertFalse(destinations.contains(new Position(3, 4))); // one step straight
        assertFalse(destinations.contains(new Position(5, 5))); // one step diagonal
        assertFalse(destinations.contains(new Position(6, 4))); // two steps straight
        assertFalse(destinations.contains(new Position(4, 4))); // same cell
    }
}
