import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class MoveValidatorTest {
    private String[][] initialGrid;
    private Board board;
    private MoveValidator validator;

    @Before
    public void setUp() {
        initialGrid = new String[][] {
            {"wR", "wN", "wB", "wQ", "wK", "wB", "wN", "wR"},
            {"wP", "wP", "wP", "wP", "wP", "wP", "wP", "wP"},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {"bP", "bP", "bP", "bP", "bP", "bP", "bP", "bP"},
            {"bR", "bN", "bB", "bQ", "bK", "bB", "bN", "bR"}
        };
        board = new Board(initialGrid);
        validator = new MoveValidator(board);
    }

    @Test
    public void testKingMovement() {
        assertTrue(validator.isValidMove(Piece.KING, Piece.WHITE, 0, 4, 0, 5));
        assertTrue(validator.isValidMove(Piece.KING, Piece.WHITE, 0, 4, 1, 4));
        assertFalse(validator.isValidMove(Piece.KING, Piece.WHITE, 0, 4, 0, 6));
    }

    @Test
    public void testRookMovement() {
        board.setPieceAt(3, 3, "wR");
        assertTrue(validator.isValidMove(Piece.ROOK, Piece.WHITE, 3, 3, 3, 5));
        assertTrue(validator.isValidMove(Piece.ROOK, Piece.WHITE, 3, 3, 5, 3));
        assertFalse(validator.isValidMove(Piece.ROOK, Piece.WHITE, 3, 3, 5, 5));
    }

    @Test
    public void testRookBlocked() {
        board.setPieceAt(3, 3, "wR");
        board.setPieceAt(3, 4, "wP");
        assertFalse(validator.isValidMove(Piece.ROOK, Piece.WHITE, 3, 3, 3, 5));
    }

    @Test
    public void testBishopMovement() {
        board.setPieceAt(3, 3, "wB");
        assertTrue(validator.isValidMove(Piece.BISHOP, Piece.WHITE, 3, 3, 5, 5));
        assertTrue(validator.isValidMove(Piece.BISHOP, Piece.WHITE, 3, 3, 1, 1));
        assertFalse(validator.isValidMove(Piece.BISHOP, Piece.WHITE, 3, 3, 3, 5));
    }

    @Test
    public void testBishopBlocked() {
        board.setPieceAt(3, 3, "wB");
        board.setPieceAt(4, 4, "wP");
        assertFalse(validator.isValidMove(Piece.BISHOP, Piece.WHITE, 3, 3, 5, 5));
    }

    @Test
    public void testQueenMovement() {
        board.setPieceAt(3, 3, "wQ");
        assertTrue(validator.isValidMove(Piece.QUEEN, Piece.WHITE, 3, 3, 3, 5));
        assertTrue(validator.isValidMove(Piece.QUEEN, Piece.WHITE, 3, 3, 5, 5));
        assertTrue(validator.isValidMove(Piece.QUEEN, Piece.WHITE, 3, 3, 5, 3));
        assertFalse(validator.isValidMove(Piece.QUEEN, Piece.WHITE, 3, 3, 5, 4));
    }

    @Test
    public void testKnightMovement() {
        board.setPieceAt(3, 3, "wN");
        assertTrue(validator.isValidMove(Piece.KNIGHT, Piece.WHITE, 3, 3, 4, 5));
        assertTrue(validator.isValidMove(Piece.KNIGHT, Piece.WHITE, 3, 3, 5, 4));
        assertTrue(validator.isValidMove(Piece.KNIGHT, Piece.WHITE, 3, 3, 2, 1));
        assertFalse(validator.isValidMove(Piece.KNIGHT, Piece.WHITE, 3, 3, 4, 4));
    }

    @Test
    public void testKnightNotBlocked() {
        board.setPieceAt(3, 3, "wN");
        board.setPieceAt(4, 4, "bP");
        assertTrue(validator.isValidMove(Piece.KNIGHT, Piece.WHITE, 3, 3, 4, 5));
    }

    @Test
    public void testPawnForwardMove() {
        assertTrue(validator.isValidMove(Piece.PAWN, Piece.WHITE, 1, 0, 2, 0));
        board.setPieceAt(2, 0, "wP");
        assertFalse(validator.isValidMove(Piece.PAWN, Piece.WHITE, 1, 0, 2, 0));
    }

    @Test
    public void testPawnTwoSquareStart() {
        assertTrue(validator.isValidMove(Piece.PAWN, Piece.WHITE, 1, 0, 3, 0));
        board.setPieceAt(2, 0, "wP");
        assertFalse(validator.isValidMove(Piece.PAWN, Piece.WHITE, 1, 0, 3, 0));
    }

    @Test
    public void testPawnCapture() {
        board.setPieceAt(2, 1, "bP");
        assertTrue(validator.isValidMove(Piece.PAWN, Piece.WHITE, 1, 0, 2, 1));
    }

    @Test
    public void testBlackPawnMovement() {
        assertTrue(validator.isValidMove(Piece.PAWN, Piece.BLACK, 6, 0, 5, 0));
        assertTrue(validator.isValidMove(Piece.PAWN, Piece.BLACK, 6, 0, 4, 0));
    }

    @Test
    public void testSameCellMove() {
        assertFalse(validator.isValidMove(Piece.KING, Piece.WHITE, 3, 3, 3, 3));
    }

    @Test
    public void testInvalidMoveForPieceType() {
        board.setPieceAt(3, 3, "wP");
        assertFalse(validator.isValidMove(Piece.PAWN, Piece.WHITE, 3, 3, 5, 3));
    }
}
