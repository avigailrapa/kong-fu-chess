import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import features.*;
import java.io.StringReader;
import java.util.Scanner;


public class CommandProcessorTest {
    private GameEngine gameEngine;
    private CommandProcessor commandProcessor;

    @BeforeEach
    public void setUp() {
        String[][] grid = new String[][] {
            {"wR", "wN", "wB", "wQ", "wK", "wB", "wN", "wR"},
            {"wP", "wP", "wP", "wP", "wP", "wP", "wP", "wP"},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {"bP", "bP", "bP", "bP", "bP", "bP", "bP", "bP"},
            {"bR", "bN", "bB", "bQ", "bK", "bB", "bN", "bR"}
        };
        Board board = new Board(grid);
        gameEngine = new GameEngine(board);
        commandProcessor = new CommandProcessor(gameEngine);
    }

    @Test
    public void testProcessClickCommand() {
        String input = "click 0 0";
        Scanner scanner = new Scanner(new StringReader(input));
        commandProcessor.processCommands(scanner);
        
        assertTrue(gameEngine.getGameState().hasSelection());
    }

    @Test
    public void testProcessWaitCommand() {
        String input = "wait 500";
        Scanner scanner = new Scanner(new StringReader(input));
        commandProcessor.processCommands(scanner);
        
        assertEquals(500, gameEngine.getGameClock().getCurrentTime());
    }

    @Test
    public void testProcessPrintBoardCommand() {
        String input = "print board";
        Scanner scanner = new Scanner(new StringReader(input));
        
        assertDoesNotThrow(() -> commandProcessor.processCommands(scanner));
    }

    @Test
    public void testProcessMultipleCommands() {
        String input = "click 0 0\nwait 500\nclick 100 100\nwait 500";
        Scanner scanner = new Scanner(new StringReader(input));
        commandProcessor.processCommands(scanner);
        
        assertEquals(1000, gameEngine.getGameClock().getCurrentTime());
    }

    @Test
    public void testProcessJumpCommand() {
        String input = "click 400 0\njump 400 0";
        Scanner scanner = new Scanner(new StringReader(input));
        commandProcessor.processCommands(scanner);
        
        assertEquals(1, gameEngine.getGameState().getActiveJumps().size());
    }

    @Test
    public void testProcessEmptyLines() {
        String input = "click 0 0\n\nwait 500\n\n";
        Scanner scanner = new Scanner(new StringReader(input));
        commandProcessor.processCommands(scanner);
        
        assertEquals(500, gameEngine.getGameClock().getCurrentTime());
    }

    @Test
    public void testProcessInvalidCommand() {
        String input = "invalid command\nclick 0 0";
        Scanner scanner = new Scanner(new StringReader(input));
        commandProcessor.processCommands(scanner);
        
        assertTrue(gameEngine.getGameState().hasSelection());
    }

    @Test
    public void testProcessClickWithInvalidCoordinates() {
        String input = "click abc def";
        Scanner scanner = new Scanner(new StringReader(input));
        
        assertDoesNotThrow(() -> commandProcessor.processCommands(scanner));
    }

    @Test
    public void testProcessWaitWithInvalidMs() {
        String input = "wait abc";
        Scanner scanner = new Scanner(new StringReader(input));
        
        assertDoesNotThrow(() -> commandProcessor.processCommands(scanner));
    }

    @Test
    public void testProcessClickWithMissingCoordinates() {
        String input = "click 0";
        Scanner scanner = new Scanner(new StringReader(input));
        
        assertDoesNotThrow(() -> commandProcessor.processCommands(scanner));
    }

    @Test
    public void testProcessWaitWithMissingMs() {
        String input = "wait";
        Scanner scanner = new Scanner(new StringReader(input));
        
        assertDoesNotThrow(() -> commandProcessor.processCommands(scanner));
    }

    @Test
    public void testProcessMixedValidAndInvalid() {
        String input = "click 0 0\nwaited 500\nwait 500\nclick 100 100";
        Scanner scanner = new Scanner(new StringReader(input));
        commandProcessor.processCommands(scanner);
        
        assertEquals(500, gameEngine.getGameClock().getCurrentTime());
    }

    @Test
    public void testProcessGameSequence() {
        String input = "click 0 100\nclick 0 200\nwait 1000\nprint board\nwait 500";
        Scanner scanner = new Scanner(new StringReader(input));
        
        assertDoesNotThrow(() -> commandProcessor.processCommands(scanner));
    }
}
