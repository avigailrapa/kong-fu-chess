package engine;

import org.junit.jupiter.api.Test;
import src.engine.AlgebraicNotation;
import src.model.Position;

import static org.junit.jupiter.api.Assertions.*;

public class AlgebraicNotationTest {

    @Test
    public void testToSquareBottomLeftCorner() {
        assertEquals("a1", AlgebraicNotation.toSquare(new Position(7, 0)));
    }

    @Test
    public void testToSquareTopRightCorner() {
        assertEquals("h8", AlgebraicNotation.toSquare(new Position(0, 7)));
    }

    @Test
    public void testToSquareMidBoard() {
        assertEquals("e7", AlgebraicNotation.toSquare(new Position(1, 4)));
    }

    @Test
    public void testToPositionIsExactInverseOfToSquare() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position original = new Position(row, col);
                assertEquals(original, AlgebraicNotation.toPosition(AlgebraicNotation.toSquare(original)));
            }
        }
    }

    @Test
    public void testToPositionRejectsWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> AlgebraicNotation.toPosition("e"));
        assertThrows(IllegalArgumentException.class, () -> AlgebraicNotation.toPosition("e77"));
        assertThrows(IllegalArgumentException.class, () -> AlgebraicNotation.toPosition(""));
    }

    @Test
    public void testToPositionRejectsFileOutsideRange() {
        assertThrows(IllegalArgumentException.class, () -> AlgebraicNotation.toPosition("i5"));
    }

    @Test
    public void testToPositionRejectsRankOutsideRange() {
        assertThrows(IllegalArgumentException.class, () -> AlgebraicNotation.toPosition("e9"));
        assertThrows(IllegalArgumentException.class, () -> AlgebraicNotation.toPosition("e0"));
    }

    @Test
    public void testToPositionRejectsUppercaseFile() {
        assertThrows(IllegalArgumentException.class, () -> AlgebraicNotation.toPosition("E5"));
    }

    @Test
    public void testToPositionRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> AlgebraicNotation.toPosition(null));
    }

    @Test
    public void testToSquareRejectsOutOfBoundsPosition() {
        assertThrows(IllegalArgumentException.class, () -> AlgebraicNotation.toSquare(new Position(8, 0)));
        assertThrows(IllegalArgumentException.class, () -> AlgebraicNotation.toSquare(new Position(0, -1)));
    }

    @Test
    public void testToSquareRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> AlgebraicNotation.toSquare(null));
    }
}
