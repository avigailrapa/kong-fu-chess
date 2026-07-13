package model;
import src.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PositionTest {

    @Test
    public void testEqualPositions() {
        Position a = new Position(2, 3);
        Position b = new Position(2, 3);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testDifferentRowNotEqual() {
        Position a = new Position(2, 3);
        Position b = new Position(5, 3);
        assertNotEquals(a, b);
    }

    @Test
    public void testDifferentColNotEqual() {
        Position a = new Position(2, 3);
        Position b = new Position(2, 9);
        assertNotEquals(a, b);
    }

    @Test
    public void testReadableRepresentation() {
        Position position = new Position(2, 3);
        assertEquals("(2, 3)", position.toString());
    }
}
