package rules;
import src.model.*;
import src.rules.*;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class QueenRuleTest {

    private final QueenRule queenRule = new QueenRule();

    @Test
    public void testCombinesRookAndBishopMovement() {
        Board board = new Board(8, 8);
        Piece queen = new Piece("q1", Piece.Color.WHITE, Piece.Kind.QUEEN, new Position(4, 4));
        board.addPiece(queen, new Position(4, 4));

        Set<Position> destinations = queenRule.legalDestinations(board, queen);

        assertTrue(destinations.contains(new Position(0, 4)));
        assertTrue(destinations.contains(new Position(4, 0)));
        assertTrue(destinations.contains(new Position(0, 0)));
        assertTrue(destinations.contains(new Position(7, 7)));
    }

    @Test
    public void testBlockedByFriendlyPieceInBothDirectionTypes() {
        Board board = new Board(8, 8);
        Piece queen = new Piece("q1", Piece.Color.WHITE, Piece.Kind.QUEEN, new Position(0, 0));
        board.addPiece(queen, new Position(0, 0));
        board.addPiece(new Piece("f1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(0, 3)), new Position(0, 3));
        board.addPiece(new Piece("f2", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(3, 3)), new Position(3, 3));

        Set<Position> destinations = queenRule.legalDestinations(board, queen);

        assertFalse(destinations.contains(new Position(0, 3)));
        assertFalse(destinations.contains(new Position(0, 4)));
        assertFalse(destinations.contains(new Position(3, 3)));
        assertFalse(destinations.contains(new Position(4, 4)));
    }

    @Test
    public void testIncludesEnemyBlockerButNotBeyondInBothDirectionTypes() {
        Board board = new Board(8, 8);
        Piece queen = new Piece("q1", Piece.Color.WHITE, Piece.Kind.QUEEN, new Position(0, 0));
        board.addPiece(queen, new Position(0, 0));
        board.addPiece(new Piece("e1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(0, 3)), new Position(0, 3));
        board.addPiece(new Piece("e2", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(3, 3)), new Position(3, 3));

        Set<Position> destinations = queenRule.legalDestinations(board, queen);

        assertTrue(destinations.contains(new Position(0, 3)));
        assertFalse(destinations.contains(new Position(0, 4)));
        assertTrue(destinations.contains(new Position(3, 3)));
        assertFalse(destinations.contains(new Position(4, 4)));
    }
}
