package input;

import src.model.*;
import src.input.BoardMapper;

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
    public void testXInSecondColumnRangeMapsToColumnOne() {
        // Column 1 spans [CELL_WIDTH, 2*CELL_WIDTH) = [102.75, 205.5).
        assertEquals(new Position(0, 1), mapper.pixelToCell(103, 0).get());
        assertEquals(new Position(0, 1), mapper.pixelToCell(205, 0).get());
    }

    @Test
    public void testYInSecondRowRangeMapsToRowOne() {
        // Row 1 spans [CELL_HEIGHT, 2*CELL_HEIGHT) = [103.5, 207).
        assertEquals(new Position(1, 0), mapper.pixelToCell(0, 104).get());
        assertEquals(new Position(1, 0), mapper.pixelToCell(0, 206).get());
    }

    @Test
    public void testDocExamples() {
        assertEquals(new Position(0, 0), mapper.pixelToCell(50, 50).get());
        assertEquals(new Position(0, 1), mapper.pixelToCell(150, 50).get());
        assertEquals(new Position(1, 0), mapper.pixelToCell(50, 150).get());
    }

    @Test
    public void testOutsideClickBeyondBoardIsRejected() {
        // Board is 8 cells wide/tall: 8*CELL_WIDTH=822, 8*CELL_HEIGHT=828.
        assertTrue(mapper.pixelToCell(822, 0).isEmpty());
        assertTrue(mapper.pixelToCell(0, 828).isEmpty());
    }

    @Test
    public void testNegativePixelsAreRejected() {
        assertTrue(mapper.pixelToCell(-1, 0).isEmpty());
        assertTrue(mapper.pixelToCell(0, -1).isEmpty());
        assertTrue(mapper.pixelToCell(-50, -50).isEmpty());
    }

    @Test
    public void testExactBoardEdgeIsInBoundsButOneCellPastIsNot() {
        // A 2x2 board spans 2*CELL_WIDTH=205.5px by 2*CELL_HEIGHT=207px.
        BoardMapper small = new BoardMapper(2, 2);
        assertTrue(small.pixelToCell(205, 206).isPresent());
        assertTrue(small.pixelToCell(206, 0).isEmpty());
        assertTrue(small.pixelToCell(0, 207).isEmpty());
    }
}
