import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import features.*;
import org.junit.jupiter.api.BeforeEach;
public class BoardTest {
    private String[][] initialGrid;
    private Board board;

    @BeforeEach
    public void setUp() {
        initialGrid = new String[][] {
            {"wR", "wN", "wB", "wQ", "wK", "wB", "wN", "wR"},
            {"wP", "wP", "wP", "wP", "wP", "wP", "wP", "wP"},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {"bP", "bP", "bP", "bP", "bP", "bP", "bP", "bP"},
            {"bR", "bN", "bB", "bQ", "bK", "bB", "bN", "bR"}
        };
        board = new Board(initialGrid);
    }

    @Test
    public void testBoardInitialization() {
        assertEquals(8, board.getNumRows());
        assertEquals(8, board.getNumCols());
    }

    @Test
    public void testGetPieceAt() {
        assertEquals("wR", board.getPieceAt(0, 0));
        assertEquals("wK", board.getPieceAt(0, 4));
        assertEquals(".", board.getPieceAt(2, 2));
        assertEquals("bK", board.getPieceAt(7, 4));
    }

    @Test
    public void testSetPieceAt() {
        board.setPieceAt(3, 3, "wQ");
        assertEquals("wQ", board.getPieceAt(3, 3));
    }

    @Test
    public void testClearCell() {
        board.clearCell(0, 0);
        assertEquals(".", board.getPieceAt(0, 0));
    }

    @Test
    public void testIsWithinBounds() {
        assertTrue(board.isWithinBounds(0, 0));
        assertTrue(board.isWithinBounds(7, 7));
        assertTrue(board.isWithinBounds(3, 3));
        assertFalse(board.isWithinBounds(-1, 0));
        assertFalse(board.isWithinBounds(0, -1));
        assertFalse(board.isWithinBounds(8, 0));
        assertFalse(board.isWithinBounds(0, 8));
    }

    @Test
    public void testGetPieceAtOutOfBounds() {
        assertThrows(IllegalArgumentException.class, () -> board.getPieceAt(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> board.getPieceAt(8, 0));
        assertThrows(IllegalArgumentException.class, () -> board.getPieceAt(0, -1));
        assertThrows(IllegalArgumentException.class, () -> board.getPieceAt(0, 8));
    }

    @Test
    public void testBoardImmutability() {
        String[][] originalGrid = board.getGrid();
        originalGrid[0][0] = "MODIFIED";
        
        String[][] copiedGrid = board.getGrid();
        assertNotEquals("MODIFIED", copiedGrid[0][0]);
        assertEquals("wR", board.getPieceAt(0, 0));
    }

    @Test
    public void testSetPieceOutOfBounds() {
        assertThrows(IllegalArgumentException.class, () -> board.setPieceAt(-1, 0, "wQ"));
        assertThrows(IllegalArgumentException.class, () -> board.setPieceAt(8, 0, "wQ"));
    }

    @Test
    public void testClearCellOutOfBounds() {
        assertThrows(IllegalArgumentException.class, () -> board.clearCell(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> board.clearCell(8, 0));
    }

    @Test
    public void testGetGrid() {
        String[][] grid = board.getGrid();
        assertEquals("wR", grid[0][0]);
        assertEquals("bK", grid[7][4]);
    }
}
