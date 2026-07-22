package io;
import src.model.*;
import src.io.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BoardParserTest {

    private final BoardParser parser = new BoardParser();

    @Test
    public void testAcceptsRectangularBoard() {
        Board board = parser.parse(
                "wK . . .\n" +
                ". wR . .\n" +
                ". . bN .\n" +
                ". . . bK"
        );

        assertEquals(4, board.width());
        assertEquals(4, board.height());
    }

    @Test
    public void testPiecesPlacedAtCorrectPositions() {
        Board board = parser.parse(
                "wK . bR\n" +
                ". . .\n" +
                ". wN bK"
        );

        Optional<Piece> king = board.pieceAt(new Position(0, 0));
        assertTrue(king.isPresent());
        assertEquals(Piece.Color.WHITE, king.get().color());
        assertEquals(Piece.Kind.KING, king.get().kind());

        Optional<Piece> rook = board.pieceAt(new Position(0, 2));
        assertTrue(rook.isPresent());
        assertEquals(Piece.Color.BLACK, rook.get().color());
        assertEquals(Piece.Kind.ROOK, rook.get().kind());

        assertTrue(board.pieceAt(new Position(1, 1)).isEmpty());
    }

    @Test
    public void testRejectsInconsistentRowLength() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                "wK . .\n" +
                ". wR .\n" +
                ". . bN ."
        ));
    }

    @Test
    public void testRejectsIllegalPieceToken() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("xK . ."));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("wZ . ."));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("w . ."));
    }

    @Test
    public void testRejectsEmptyText() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("   \n   "));
    }

    @Test
    public void testAcceptsSingleCellBoard() {
        Board board = parser.parse("wK");

        assertEquals(1, board.width());
        assertEquals(1, board.height());
        assertTrue(board.pieceAt(new Position(0, 0)).isPresent());
    }

    @Test
    public void testAcceptsSingleRowBoard() {
        Board board = parser.parse("wK . . bK");

        assertEquals(4, board.width());
        assertEquals(1, board.height());
    }

    @Test
    public void testAcceptsSingleColumnBoard() {
        Board board = parser.parse("wK\n.\n.\nbK");

        assertEquals(1, board.width());
        assertEquals(4, board.height());
    }
}
