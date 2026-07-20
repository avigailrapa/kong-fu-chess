package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import src.model.*;

public class PieceTest {

    @Test
    public void testCreatePieceStartsIdleAtGivenCell() {
        Position cell = new Position(0, 0);
        Piece piece = new Piece("w1", Piece.Color.WHITE, Piece.Kind.KING, cell);

        assertEquals("w1", piece.getId());
        assertEquals(Piece.Color.WHITE, piece.getColor());
        assertEquals(Piece.Kind.KING, piece.getKind());
        assertEquals(cell, piece.getCell());
        assertEquals(Piece.State.IDLE, piece.getState());
    }

    @Test
    public void testStateCanBecomeMoving() {
        Piece piece = new Piece("w1", Piece.Color.WHITE, Piece.Kind.PAWN, new Position(1, 0));
        piece.setState(Piece.State.MOVING);
        assertEquals(Piece.State.MOVING, piece.getState());
    }

    @Test
    public void testStateCanBecomeCaptured() {
        Piece piece = new Piece("b1", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(6, 0));
        piece.setState(Piece.State.CAPTURED);
        assertEquals(Piece.State.CAPTURED, piece.getState());
    }

    @Test
    public void testSetCellUpdatesOnlyPosition() {
        Piece piece = new Piece("w1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));
        piece.setCell(new Position(0, 5));

        assertEquals(new Position(0, 5), piece.getCell());
        assertEquals(Piece.Kind.ROOK, piece.getKind());
        assertEquals(Piece.Color.WHITE, piece.getColor());
    }

    @Test
    public void testEqualityIsBasedOnIdOnly() {
        Piece a = new Piece("shared-id", Piece.Color.WHITE, Piece.Kind.QUEEN, new Position(0, 0));
        Piece b = new Piece("shared-id", Piece.Color.BLACK, Piece.Kind.PAWN, new Position(5, 5));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testDifferentIdsAreNotEqual() {
        Piece a = new Piece("id-1", Piece.Color.WHITE, Piece.Kind.KING, new Position(0, 0));
        Piece b = new Piece("id-2", Piece.Color.WHITE, Piece.Kind.KING, new Position(0, 0));

        assertNotEquals(a, b);
    }

    @Test
    public void testConstructorRejectsNulls() {
        Position cell = new Position(0, 0);
        assertThrows(NullPointerException.class,
                () -> new Piece(null, Piece.Color.WHITE, Piece.Kind.KING, cell));
        assertThrows(NullPointerException.class,
                () -> new Piece("id", null, Piece.Kind.KING, cell));
        assertThrows(NullPointerException.class,
                () -> new Piece("id", Piece.Color.WHITE, null, cell));
        assertThrows(NullPointerException.class,
                () -> new Piece("id", Piece.Color.WHITE, Piece.Kind.KING, null));
    }

    @Test
    public void testSetCellRejectsNull() {
        Piece piece = new Piece("w1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));
        assertThrows(NullPointerException.class, () -> piece.setCell(null));
    }

    @Test
    public void testSetStateRejectsNull() {
        Piece piece = new Piece("w1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0));
        assertThrows(NullPointerException.class, () -> piece.setState(null));
    }

    @Test
    public void testColorFromLetterIsCaseInsensitiveInverseOfLetter() {
        for (Piece.Color color : Piece.Color.values()) {
            assertEquals(color, Piece.Color.fromLetter(color.letter()));
            assertEquals(color, Piece.Color.fromLetter(Character.toLowerCase(color.letter())));
        }
    }

    @Test
    public void testColorFromLetterRejectsUnknownLetter() {
        assertThrows(IllegalArgumentException.class, () -> Piece.Color.fromLetter('X'));
    }

    @Test
    public void testKindFromLetterIsCaseInsensitiveInverseOfLetter() {
        for (Piece.Kind kind : Piece.Kind.values()) {
            assertEquals(kind, Piece.Kind.fromLetter(kind.letter()));
            assertEquals(kind, Piece.Kind.fromLetter(Character.toLowerCase(kind.letter())));
        }
    }

    @Test
    public void testKindFromLetterRejectsUnknownLetter() {
        assertThrows(IllegalArgumentException.class, () -> Piece.Kind.fromLetter('X'));
    }
}
