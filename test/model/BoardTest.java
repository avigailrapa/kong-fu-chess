package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import src.model.*;


public class BoardTest {

    private Board board;

    @BeforeEach
    public void setUp() {
        board = new Board(8, 8);
    }

    @Test
    public void testDimensionsAreInferredCorrectly() {
        assertEquals(8, board.getWidth());
        assertEquals(8, board.getHeight());
    }

    @Test
    public void testConstructorRejectsNonPositiveDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new Board(0, 8));
        assertThrows(IllegalArgumentException.class, () -> new Board(8, 0));
        assertThrows(IllegalArgumentException.class, () -> new Board(-1, 8));
    }

    @Test
    public void testIsWithinBorder() {
        assertTrue(board.isWithinBorder(new Position(0, 0)));
        assertTrue(board.isWithinBorder(new Position(7, 7)));
        assertFalse(board.isWithinBorder(new Position(-1, 0)));
        assertFalse(board.isWithinBorder(new Position(0, -1)));
        assertFalse(board.isWithinBorder(new Position(8, 0)));
        assertFalse(board.isWithinBorder(new Position(0, 8)));
    }

    @Test
    public void testEmptyCellReturnsNoPiece() {
        Optional<Piece> result = board.getPieceAt(new Position(3, 3));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOccupiedCellReturnsCorrectPiece() {
        Position cell = new Position(0, 4);
        Piece king = new Piece("wK1", Piece.Color.WHITE, Piece.Kind.KING, cell);
        board.addPiece(king, cell);

        Optional<Piece> result = board.getPieceAt(cell);
        assertTrue(result.isPresent());
        assertEquals(king, result.get());
    }

    @Test
    public void testAddingTwoPiecesToSameCellFails() {
        Position cell = new Position(0, 0);
        board.addPiece(new Piece("w1", Piece.Color.WHITE, Piece.Kind.ROOK, cell), cell);

        assertThrows(IllegalStateException.class,
                () -> board.addPiece(new Piece("w2", Piece.Color.WHITE, Piece.Kind.KNIGHT, cell), cell));
    }

    @Test
    public void testAddingDuplicateIdToDifferentCellFails() {
        board.addPiece(new Piece("dup", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0)), new Position(0, 0));

        assertThrows(IllegalStateException.class,
                () -> board.addPiece(new Piece("dup", Piece.Color.BLACK, Piece.Kind.KNIGHT, new Position(5, 5)), new Position(5, 5)));
    }

    @Test
    public void testMovingPieceUpdatesSourceAndDestination() {
        Position from = new Position(0, 0);
        Position to = new Position(0, 5);
        Piece rook = new Piece("w1", Piece.Color.WHITE, Piece.Kind.ROOK, from);
        board.addPiece(rook, from);

        board.movePiece(from, to);

        assertTrue(board.getPieceAt(from).isEmpty());
        assertEquals(rook, board.getPieceAt(to).get());
        assertEquals(to, rook.getCell());
    }

    @Test
    public void testMovingFromEmptyCellFails() {
        assertThrows(IllegalStateException.class,
                () -> board.movePiece(new Position(1, 1), new Position(2, 2)));
    }

    @Test
    public void testMovingToOccupiedCellFails() {
        Position from = new Position(0, 0);
        Position to = new Position(0, 1);
        board.addPiece(new Piece("w1", Piece.Color.WHITE, Piece.Kind.ROOK, from), from);
        board.addPiece(new Piece("w2", Piece.Color.WHITE, Piece.Kind.KNIGHT, to), to);

        assertThrows(IllegalStateException.class, () -> board.movePiece(from, to));
    }

    @Test
    public void testRemovingPieceClearsItsCell() {
        Position cell = new Position(3, 3);
        Piece piece = new Piece("b1", Piece.Color.BLACK, Piece.Kind.PAWN, cell);
        board.addPiece(piece, cell);

        Piece removed = board.removePiece(cell);

        assertEquals(piece, removed);
        assertTrue(board.getPieceAt(cell).isEmpty());
    }

    @Test
    public void testRemovingFromEmptyCellFails() {
        assertThrows(IllegalStateException.class, () -> board.removePiece(new Position(2, 2)));
    }

    @Test
    public void testOutOfBoundsAccessThrows() {
        assertThrows(IllegalArgumentException.class, () -> board.getPieceAt(new Position(-1, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> board.addPiece(new Piece("x", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(9, 9)), new Position(9, 9)));
        assertThrows(IllegalArgumentException.class, () -> board.removePiece(new Position(9, 9)));
    }

    @Test
    public void testOccupiedPositionsReturnsExactSetOfOccupiedCells() {
        Position a = new Position(0, 0);
        Position b = new Position(3, 5);
        board.addPiece(new Piece("w1", Piece.Color.WHITE, Piece.Kind.ROOK, a), a);
        board.addPiece(new Piece("w2", Piece.Color.WHITE, Piece.Kind.KNIGHT, b), b);

        assertEquals(Set.of(a, b), board.occupiedPositions());
    }

    @Test
    public void testOccupiedPositionsIsEmptyOnEmptyBoard() {
        assertTrue(board.occupiedPositions().isEmpty());
    }
}
