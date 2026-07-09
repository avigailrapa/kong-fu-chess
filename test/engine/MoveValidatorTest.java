import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import model.*;

public class MoveValidatorTest {
    private String[][] initialGrid;
    private IBoard board;
    private MoveValidator validator;
@BeforeEach
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
    }

    @Test
    public void testKnightMovement() {
        assertTrue(validator.isValidMove(Piece.KNIGHT, Piece.WHITE, 0, 1, 2, 2));
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
}