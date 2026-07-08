import org.junit.Before;
import org.junit.Test;
import java.io.StringReader;
import java.util.Scanner;
import java.util.List;
import static org.junit.Assert.*;

public class BoardReaderTest {

    @Test
    public void testReadValidBoard() {
        String input = "Board:\nwR wN\nbR bN\nCommands:";
        Scanner scanner = new Scanner(new StringReader(input));
        
        List<String[]> rows = BoardReader.readBoard(scanner);
        
        assertNotNull(rows);
        assertEquals(2, rows.size());
        assertEquals("wR", rows.get(0)[0]);
        assertEquals("wN", rows.get(0)[1]);
    }

    @Test
    public void testReadBoardWithEmptyLines() {
        String input = "Board:\nwR wN\n\nbR bN\nCommands:";
        Scanner scanner = new Scanner(new StringReader(input));
        
        List<String[]> rows = BoardReader.readBoard(scanner);
        
        assertNotNull(rows);
        assertEquals(2, rows.size());
    }

    @Test
    public void testReadBoardStopsAtCommands() {
        String input = "Board:\nwR wN\nCommands:\nclick 0 0";
        Scanner scanner = new Scanner(new StringReader(input));
        
        List<String[]> rows = BoardReader.readBoard(scanner);
        
        assertNotNull(rows);
        assertEquals(1, rows.size());
    }

    @Test
    public void testReadBoardWithInvalidToken() {
        String input = "Board:\nwR xN\nCommands:";
        Scanner scanner = new Scanner(new StringReader(input));
        
        List<String[]> rows = BoardReader.readBoard(scanner);
        
        assertNull(rows);
    }

    @Test
    public void testReadBoardMismatchedWidth() {
        String input = "Board:\nwR wN wB\nwP wP\nCommands:";
        Scanner scanner = new Scanner(new StringReader(input));
        
        List<String[]> rows = BoardReader.readBoard(scanner);
        
        assertNull(rows);
    }

    @Test
    public void testReadBoardWithEmptyInput() {
        String input = "";
        Scanner scanner = new Scanner(new StringReader(input));
        
        List<String[]> rows = BoardReader.readBoard(scanner);
        
        assertNull(rows);
    }

    @Test
    public void testReadBoardWithOnlyCommands() {
        String input = "Commands:";
        Scanner scanner = new Scanner(new StringReader(input));
        
        List<String[]> rows = BoardReader.readBoard(scanner);
        
        assertNull(rows);
    }

    @Test
    public void testCreateBoardFromValidRows() {
        List<String[]> rows = new java.util.ArrayList<>();
        rows.add(new String[]{"wR", "wN"});
        rows.add(new String[]{"bR", "bN"});
        
        Board board = BoardReader.createBoardFromRows(rows);
        
        assertNotNull(board);
        assertEquals(2, board.getNumRows());
        assertEquals(2, board.getNumCols());
    }

    @Test
    public void testCreateBoardFromNull() {
        Board board = BoardReader.createBoardFromRows(null);
        assertNull(board);
    }

    @Test
    public void testCreateBoardFromEmptyList() {
        List<String[]> rows = new java.util.ArrayList<>();
        Board board = BoardReader.createBoardFromRows(rows);
        assertNull(board);
    }

    @Test
    public void testReadFullChessboard() {
        String input = "Board:\n" +
            "wR wN wB wQ wK wB wN wR\n" +
            "wP wP wP wP wP wP wP wP\n" +
            ". . . . . . . .\n" +
            ". . . . . . . .\n" +
            ". . . . . . . .\n" +
            ". . . . . . . .\n" +
            "bP bP bP bP bP bP bP bP\n" +
            "bR bN bB bQ bK bB bN bR\n" +
            "Commands:";
        Scanner scanner = new Scanner(new StringReader(input));
        
        List<String[]> rows = BoardReader.readBoard(scanner);
        
        assertNotNull(rows);
        assertEquals(8, rows.size());
        assertEquals(8, rows.get(0).length);
        assertEquals("wK", rows.get(0)[4]);
        assertEquals("bK", rows.get(7)[4]);
    }

    @Test
    public void testBoardReaderWithAllPieceTypes() {
        String input = "Board:\nwK wQ wR wB wN wP . bK\nCommands:";
        Scanner scanner = new Scanner(new StringReader(input));
        
        List<String[]> rows = BoardReader.readBoard(scanner);
        
        assertNotNull(rows);
        assertEquals(1, rows.size());
        assertEquals(8, rows.get(0).length);
    }

    @Test
    public void testReadBoardWithWhitespace() {
        String input = "Board:\n  wR  wN  \n  bR  bN  \nCommands:";
        Scanner scanner = new Scanner(new StringReader(input));
        
        List<String[]> rows = BoardReader.readBoard(scanner);
        
        assertNotNull(rows);
        assertEquals(2, rows.size());
        assertEquals("wR", rows.get(0)[0]);
    }
}
