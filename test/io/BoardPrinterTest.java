package io;

import model.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BoardPrinterTest {

    private final BoardParser parser = new BoardParser();
    private final BoardPrinter printer = new BoardPrinter();

    @Test
    public void testRoundTripsSimpleBoard() {
        String text =
                ". . .\n" +
                ". wK .\n" +
                ". . .";

        Board board = parser.parse(text);

        assertEquals(text, printer.print(board));
    }

    @Test
    public void testRoundTripsBoardWithMultiplePieces() {
        String text =
                "wK . bR\n" +
                ". . .\n" +
                ". wN bK";

        Board board = parser.parse(text);

        assertEquals(text, printer.print(board));
    }

    @Test
    public void testPrintsAllEmptyBoard() {
        Board board = new Board(3, 2);

        assertEquals(". . .\n. . .", printer.print(board));
    }
}
