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

    private void runScript(String fileName) throws IOException {
        String script = Files.readString(SCRIPTS_DIR.resolve(fileName));
        assertDoesNotThrow(() -> new ScriptRunner().run(script));
    }
}
