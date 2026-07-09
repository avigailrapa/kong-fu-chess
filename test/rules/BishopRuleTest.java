package rules;

import model.Board;
import model.Piece;
import model.Position;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BishopRuleTest {

    private final BishopRule bishopRule = new BishopRule();

    @Test
    public void testMovesDiagonallyOnEmptyBoard() {
        Board board = new Board(8, 8);
        Piece bishop = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(4, 4));
        board.addPiece(bishop, new Position(4, 4));

        Set<Position> destinations = bishopRule.legalDestinations(board, bishop);

        assertTrue(destinations.contains(new Position(0, 0)));
        assertTrue(destinations.contains(new Position(7, 7)));
        assertTrue(destinations.contains(new Position(1, 7)));
        assertTrue(destinations.contains(new Position(7, 1)));
    }

    @Test
    public void testDoesNotMoveStraight() {
        Board board = new Board(8, 8);
        Piece bishop = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(4, 4));
        board.addPiece(bishop, new Position(4, 4));

        Set<Position> destinations = bishopRule.legalDestinations(board, bishop);

        assertFalse(destinations.contains(new Position(4, 0)));
        assertFalse(destinations.contains(new Position(0, 4)));
        assertFalse(destinations.contains(new Position(4, 7)));
        assertFalse(destinations.contains(new Position(7, 4)));
    }

    @Test
    public void testBlockedByFriendlyPiece() {
        Board board = new Board(8, 8);
        Piece bishop = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(0, 0));
        board.addPiece(bishop, new Position(0, 0));
        board.addPiece(new Piece("f1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(3, 3)), new Position(3, 3));

        Set<Position> destinations = bishopRule.legalDestinations(board, bishop);

        assertTrue(destinations.contains(new Position(2, 2)));
        assertFalse(destinations.contains(new Position(3, 3)));
        assertFalse(destinations.contains(new Position(4, 4)));
    }

    @Test
    public void testIncludesEnemyBlockerButNotBeyond() {
        Board board = new Board(8, 8);
        Piece bishop = new Piece("b1", Piece.Color.WHITE, Piece.Kind.BISHOP, new Position(0, 0));
        board.addPiece(bishop, new Position(0, 0));
        board.addPiece(new Piece("e1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(3, 3)), new Position(3, 3));

        Set<Position> destinations = bishopRule.legalDestinations(board, bishop);

        assertTrue(destinations.contains(new Position(3, 3)));
        assertFalse(destinations.contains(new Position(4, 4)));
    }
}
