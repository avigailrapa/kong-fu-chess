package io;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.StringReader;
import java.util.Scanner;
import java.util.List;

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
}