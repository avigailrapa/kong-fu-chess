package rules;

import model.Board;
import model.Piece;
import model.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RuleEngineTest {

    private Board board;
    private RuleEngine ruleEngine;

    @BeforeEach
    public void setUp() {
        board = new Board(8, 8);
        ruleEngine = new RuleEngine(Map.of(Piece.Kind.ROOK, new RookRule()));
    }

    @Test
    public void testValidRookMoveIsOk() {
        Position source = new Position(0, 0);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, source), source);

        MoveValidation result = ruleEngine.validateMove(board, source, new Position(0, 5));

        assertTrue(result.isValid());
        assertEquals("ok", result.reason());
    }

    @Test
    public void testSourceOutsideBoardIsRejected() {
        MoveValidation result = ruleEngine.validateMove(board, new Position(-1, 0), new Position(0, 0));

        assertFalse(result.isValid());
        assertEquals("outside_board", result.reason());
    }

    @Test
    public void testDestinationOutsideBoardIsRejected() {
        Position source = new Position(0, 0);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, source), source);

        MoveValidation result = ruleEngine.validateMove(board, source, new Position(8, 0));

        assertFalse(result.isValid());
        assertEquals("outside_board", result.reason());
    }

    @Test
    public void testEmptySourceIsRejected() {
        MoveValidation result = ruleEngine.validateMove(board, new Position(3, 3), new Position(4, 4));

        assertFalse(result.isValid());
        assertEquals("empty_source", result.reason());
    }

    @Test
    public void testFriendlyDestinationIsRejected() {
        Position source = new Position(0, 0);
        Position destination = new Position(0, 3);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, source), source);
        board.addPiece(new Piece("f1", Piece.Color.WHITE, Piece.Kind.PAWN, destination), destination);

        MoveValidation result = ruleEngine.validateMove(board, source, destination);

        assertFalse(result.isValid());
        assertEquals("friendly_destination", result.reason());
    }

    @Test
    public void testIllegalPieceMoveIsRejected() {
        Position source = new Position(0, 0);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, source), source);

        MoveValidation result = ruleEngine.validateMove(board, source, new Position(1, 1));

        assertFalse(result.isValid());
        assertEquals("illegal_piece_move", result.reason());
    }

    @Test
    public void testEnemyDestinationIsAllowedWhenReachable() {
        Position source = new Position(0, 0);
        Position destination = new Position(0, 3);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, source), source);
        board.addPiece(new Piece("e1", Piece.Color.BLACK, Piece.Kind.PAWN, destination), destination);

        MoveValidation result = ruleEngine.validateMove(board, source, destination);

        assertTrue(result.isValid());
    }
}
