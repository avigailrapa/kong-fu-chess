package io;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.StringReader;
import java.util.Scanner;
import model.*;
import engine.*;

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
    public void testProcessMixedValidAndInvalid() {
        String input = "click 0 0\nwaited 500\nwait 500\nclick 100 100";
        Scanner scanner = new Scanner(new StringReader(input));
        commandProcessor.processCommands(scanner);
        
        assertEquals(500, gameEngine.getGameClock().getCurrentTime());
    }
}