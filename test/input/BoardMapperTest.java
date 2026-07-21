package input;

import src.model.*;
import src.input.BoardMapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BoardMapperTest {

    private final BoardMapper mapper = new BoardMapper(8, 8);

    @Test
    public void testXZeroToNinetyTwoMapsToColumnZero() {
        assertEquals(new Position(0, 0), mapper.pixelToCell(0, 0).get());
        assertEquals(new Position(0, 0), mapper.pixelToCell(92, 0).get());
    }

    @Test
    public void testXInSecondColumnRangeMapsToColumnOne() {
        assertEquals(new Position(0, 1), mapper.pixelToCell(93, 0).get());
        assertEquals(new Position(0, 1), mapper.pixelToCell(184, 0).get());
    }

    @Test
    public void testYInSecondRowRangeMapsToRowOne() {
        assertEquals(new Position(1, 0), mapper.pixelToCell(0, 94).get());
        assertEquals(new Position(1, 0), mapper.pixelToCell(0, 186).get());
    }

    @Test
    public void testDocExamples() {
        assertEquals(new Position(0, 0), mapper.pixelToCell(50, 50).get());
        assertEquals(new Position(0, 1), mapper.pixelToCell(150, 50).get());
        assertEquals(new Position(1, 0), mapper.pixelToCell(50, 150).get());
    }

    @Test
    public void testOutsideClickBeyondBoardIsRejected() {
        assertTrue(mapper.pixelToCell(740, 0).isEmpty());
        assertTrue(mapper.pixelToCell(0, 746).isEmpty());
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
        assertTrue(small.pixelToCell(184, 186).isPresent());
        assertTrue(small.pixelToCell(185, 0).isEmpty());
        assertTrue(small.pixelToCell(0, 187).isEmpty());
    }
}
