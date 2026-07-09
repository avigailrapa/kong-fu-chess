package input;

import model.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BoardMapperTest {

    private final BoardMapper mapper = new BoardMapper(8, 8);

    @Test
    public void testXZeroToNinetyNineMapsToColumnZero() {
        assertEquals(new Position(0, 0), mapper.pixelToCell(0, 0).get());
        assertEquals(new Position(0, 0), mapper.pixelToCell(99, 0).get());
    }

    @Test
    public void testXOneHundredToOneNinetyNineMapsToColumnOne() {
        assertEquals(new Position(0, 1), mapper.pixelToCell(100, 0).get());
        assertEquals(new Position(0, 1), mapper.pixelToCell(199, 0).get());
    }

    @Test
    public void testYOneHundredToOneNinetyNineMapsToRowOne() {
        assertEquals(new Position(1, 0), mapper.pixelToCell(0, 100).get());
        assertEquals(new Position(1, 0), mapper.pixelToCell(0, 199).get());
    }

    @Test
    public void testDocExamples() {
        assertEquals(new Position(0, 0), mapper.pixelToCell(50, 50).get());
        assertEquals(new Position(0, 1), mapper.pixelToCell(150, 50).get());
        assertEquals(new Position(1, 0), mapper.pixelToCell(50, 150).get());
    }

    @Test
    public void testOutsideClickBeyondBoardIsRejected() {
        assertTrue(mapper.pixelToCell(800, 0).isEmpty());
        assertTrue(mapper.pixelToCell(0, 800).isEmpty());
    }

    @Test
    public void testNegativePixelsAreRejected() {
        assertTrue(mapper.pixelToCell(-1, 0).isEmpty());
        assertTrue(mapper.pixelToCell(0, -1).isEmpty());
        assertTrue(mapper.pixelToCell(-50, -50).isEmpty());
    }

    @Test
    public void testExactBoardEdgeIsInBoundsButOneCellPastIsNot() {
        BoardMapper small = new BoardMapper(2, 2);
        assertTrue(small.pixelToCell(199, 199).isPresent());
        assertTrue(small.pixelToCell(200, 0).isEmpty());
        assertTrue(small.pixelToCell(0, 200).isEmpty());
    }
}
