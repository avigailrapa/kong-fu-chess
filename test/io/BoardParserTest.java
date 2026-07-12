package io;

import model.Board;
import model.Piece;
import model.Position;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BoardParserTest {

    private final BoardParser parser = new BoardParser();

    @Test
    public void testAcceptsRectangularBoard() {
        Board board = parser.parse(
                "wK . .\n" +
                ". wR . .\n" +
                ". . bN .\n" +
                ". . . bK"
        );

        assertEquals(4, board.getWidth());
        assertEquals(4, board.getHeight());
    }

    @Test
    public void testPiecesPlacedAtCorrectPositions() {
        Board board = parser.parse(
                "wK . bR\n" +
                ". . .\n" +
                ". wN bK"
        );

        Optional<Piece> king = board.getPieceAt(new Position(0, 0));
        assertTrue(king.isPresent());
        assertEquals(Piece.Color.WHITE, king.get().getColor());
        assertEquals(Piece.Kind.KING, king.get().getKind());

        Optional<Piece> rook = board.getPieceAt(new Position(0, 2));
        assertTrue(rook.isPresent());
        assertEquals(Piece.Color.BLACK, rook.get().getColor());
        assertEquals(Piece.Kind.ROOK, rook.get().getKind());

        assertTrue(board.getPieceAt(new Position(1, 1)).isEmpty());
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

        assertEquals(1, board.getWidth());
        assertEquals(1, board.getHeight());
        assertTrue(board.getPieceAt(new Position(0, 0)).isPresent());
    }

    @Test
    public void testAcceptsSingleRowBoard() {
        Board board = parser.parse("wK . . bK");

        assertEquals(4, board.getWidth());
        assertEquals(1, board.getHeight());
    }

    @Test
    public void testAcceptsSingleColumnBoard() {
        Board board = parser.parse("wK\n.\n.\nbK");

        assertEquals(1, board.getWidth());
        assertEquals(4, board.getHeight());
    }
}
