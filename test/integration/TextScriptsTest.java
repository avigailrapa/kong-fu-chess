package integration;

import org.junit.jupiter.api.Test;
import texttests.ScriptRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class TextScriptsTest {

    private static final Path SCRIPTS_DIR = Path.of("test", "integration", "scripts");

    @Test
    public void testBoardParsingScript() throws IOException {
        runScript("01_board_parsing.kfc");
    }

    @Test
    public void testClickToMoveScript() throws IOException {
        runScript("02_click_to_move.kfc");
    }

    @Test
    public void testRookMovesScript() throws IOException {
        runScript("03_rook_moves.kfc");
    }

    @Test
    public void testInvalidMovesScript() throws IOException {
        runScript("04_invalid_moves.kfc");
    }

    @Test
    public void testCaptureScript() throws IOException {
        runScript("05_capture.kfc");
    }

    @Test
    public void testGameOverScript() throws IOException {
        runScript("06_game_over.kfc");
    }

    private void runScript(String fileName) throws IOException {
        String script = Files.readString(SCRIPTS_DIR.resolve(fileName));
        assertDoesNotThrow(() -> new ScriptRunner().run(script));
    }
}
