package model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PieceTest {

    @Test
    public void testCreatePiece() {
        Piece piece = Piece.create(Piece.WHITE, Piece.KING);
        assertEquals(Piece.WHITE, piece.getColor());
        assertEquals(Piece.KING, piece.getType());
    }

    @Test
    public void testCreateAllPieceTypes() {
        Piece.create(Piece.WHITE, Piece.KING);
        Piece.create(Piece.WHITE, Piece.QUEEN);
        Piece.create(Piece.WHITE, Piece.ROOK);
        Piece.create(Piece.WHITE, Piece.BISHOP);
        Piece.create(Piece.WHITE, Piece.KNIGHT);
        Piece.create(Piece.WHITE, Piece.PAWN);
    }

    @Test
    public void testFromString() {
        Piece piece = Piece.fromString("wK");
        assertEquals(Piece.WHITE, piece.getColor());
        assertEquals(Piece.KING, piece.getType());

        Piece blackPawn = Piece.fromString("bP");
        assertEquals(Piece.BLACK, blackPawn.getColor());
        assertEquals(Piece.PAWN, blackPawn.getType());
    }

    @Test
    public void testFromStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Piece.fromString("."));
        assertThrows(IllegalArgumentException.class, () -> Piece.fromString("xK"));
        assertThrows(IllegalArgumentException.class, () -> Piece.fromString("wX"));
        assertThrows(IllegalArgumentException.class, () -> Piece.fromString("w"));
    }

    @Test
    public void testIsValidToken() {
        assertTrue(Piece.isValidToken("."));
        assertTrue(Piece.isValidToken("wK"));
        assertTrue(Piece.isValidToken("bP"));
        assertFalse(Piece.isValidToken("xK"));
        assertFalse(Piece.isValidToken("wX"));
        assertFalse(Piece.isValidToken(""));
        assertFalse(Piece.isValidToken(null));
        assertFalse(Piece.isValidToken("wKK"));
    }

    @Test
    public void testIsWhite() {
        Piece white = Piece.create(Piece.WHITE, Piece.KING);
        Piece black = Piece.create(Piece.BLACK, Piece.KING);
        assertTrue(white.isWhite());
        assertFalse(black.isWhite());
    }

    @Test
    public void testIsBlack() {
        Piece white = Piece.create(Piece.WHITE, Piece.KING);
        Piece black = Piece.create(Piece.BLACK, Piece.KING);
        assertFalse(white.isBlack());
        assertTrue(black.isBlack());
    }

    @Test
    public void testIsSameColorAs() {
        Piece white1 = Piece.create(Piece.WHITE, Piece.KING);
        Piece white2 = Piece.create(Piece.WHITE, Piece.QUEEN);
        Piece black = Piece.create(Piece.BLACK, Piece.KING);

        assertTrue(white1.isSameColorAs(white2));
        assertFalse(white1.isSameColorAs(black));
        assertFalse(black.isSameColorAs(white1));
    }

    @Test
    public void testIsKing() {
        Piece king = Piece.create(Piece.WHITE, Piece.KING);
        Piece queen = Piece.create(Piece.WHITE, Piece.QUEEN);
        assertTrue(king.isKing());
        assertFalse(queen.isKing());
    }

    @Test
    public void testCreateInvalidColor() {
        assertThrows(IllegalArgumentException.class, () -> Piece.create('x', Piece.KING));
    }
}