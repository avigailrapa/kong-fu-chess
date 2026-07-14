package integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import src.input.CommandParser;
import src.input.CommandRunner;
import src.input.ConsoleRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextScriptsTest {

    private static final Path SCRIPTS_DIR = Path.of("test", "integration", "scripts");

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    public void captureOutput() {
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    public void restoreStream() {
        System.setOut(originalOut);
    }

    private String run(String fileName) throws IOException {
        String script = Files.readString(SCRIPTS_DIR.resolve(fileName));
        new ConsoleRunner(new CommandParser(), new CommandRunner()).run(script);
        return capturedOut.toString(StandardCharsets.UTF_8).replace("\r\n", "\n").trim();
    }

    @Test
    public void testBoardParsingScript() throws IOException {
        assertEquals(". . .\n. wK .\n. . .", run("01_board_parsing.kfc"));
    }

    @Test
    public void testClickToMoveScript() throws IOException {
        assertEquals(
                ". wR .\n. . .\n. . bK\n" +
                        ". . .\n. . .\n. wR bK",
                run("02_click_to_move.kfc"));
    }

    @Test
    public void testRookMovesScript() throws IOException {
        assertEquals(". . wR\n. . .\n. . bK", run("03_rook_moves.kfc"));
    }

    @Test
    public void testInvalidMovesScript() throws IOException {
        assertEquals("wR wP .\n. . .\n. . bK", run("04_invalid_moves.kfc"));
    }

    @Test
    public void testCaptureScript() throws IOException {
        assertEquals(". . wR\n. . .\n. . bK", run("05_capture.kfc"));
    }

    @Test
    public void testGameOverScript() throws IOException {
        assertEquals(
                ". . wR\n. . wN\n. . .\n" +
                        ". . wR\n. . wN\n. . .",
                run("06_game_over.kfc"));
    }

    @Test
    public void testSameCellClickScript() throws IOException {
        assertEquals(". wR .\n. . .\n. . bK", run("07_same_cell_click.kfc"));
    }
}
