package app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MainTest {

    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    public void captureOutput() {
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    public void restoreStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    private String run(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        Main.main(new String[0]);
        return capturedOut.toString(StandardCharsets.UTF_8).replace("\r\n", "\n").trim();
    }

    @Test
    public void testBoardParsingAndPrintBoardRoundTrip() {
        String input = "Board:\n" +
                "wK . .\n" +
                ". . .\n" +
                "Commands:\n" +
                "print board\n";

        String output = run(input);

        assertEquals("wK . .\n. . .", output);
    }

    @Test
    public void testClickClickWaitMovesPieceAndPrintsUpdatedBoard() {
        String input = "Board:\n" +
                ". wR .\n" +
                ". . .\n" +
                ". . bK\n" +
                "Commands:\n" +
                "click 150 50\n" +
                "click 150 250\n" +
                "wait 2000\n" +
                "print board\n";

        String output = run(input);

        assertEquals(". . .\n. . .\n. wR bK", output);
    }

    @Test
    public void testUnknownTokenReportsErrorAndHalts() {
        String input = "Board:\n" +
                "wK xZ\n" +
                ". .\n" +
                "Commands:\n" +
                "print board\n";

        String output = run(input);

        assertEquals("ERROR UNKNOWN_TOKEN", output);
    }

    @Test
    public void testRowWidthMismatchReportsErrorAndHalts() {
        String input = "Board:\n" +
                "wK . . .\n" +
                "wR . .\n" +
                "Commands:\n" +
                "print board\n";

        String output = run(input);

        assertEquals("ERROR ROW_WIDTH_MISMATCH", output);
    }

    @Test
    public void testJumpCommandLetsDefenderSurviveExactTie() {
        String input = "Board:\n" +
                ". . .\n" +
                "wK bR .\n" +
                ". . .\n" +
                "Commands:\n" +
                "jump 50 150\n" +
                "click 150 150\n" +
                "click 50 150\n" +
                "wait 1000\n" +
                "print board\n";

        String output = run(input);

        assertEquals(". . .\nwK . .\n. . .", output);
    }

    @Test
    public void testTwoDifferentPiecesMoveSimultaneously() {
        String input = "Board:\n" +
                "wR . wN\n" +
                ". . .\n" +
                ". . bK\n" +
                "Commands:\n" +
                "click 50 50\n" +
                "click 50 150\n" +
                "click 250 50\n" +
                "click 150 250\n" +
                "wait 2000\n" +
                "print board\n";

        String output = run(input);

        assertEquals(". . .\nwR . .\n. wN bK", output);
    }
}
