package net;

import org.junit.jupiter.api.Test;
import src.engine.AlgebraicNotation;
import src.model.Piece;
import src.model.Position;
import src.net.JumpCommand;
import src.net.MalformedMessageException;
import src.net.MoveAccepted;
import src.net.MoveCommand;
import src.net.MoveRejected;
import src.net.Protocol;
import src.net.WireMessage;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocolTest {

    @Test
    public void testParseBareMoveTokenFromSlideExample() {
        WireMessage message = Protocol.parse("WQe2e5");

        assertEquals(new MoveCommand(Piece.Color.WHITE, Piece.Kind.QUEEN,
                AlgebraicNotation.toPosition("e2"), AlgebraicNotation.toPosition("e5")), message);
    }

    @Test
    public void testEncodeMoveCommandRoundTrips() {
        MoveCommand original = new MoveCommand(Piece.Color.BLACK, Piece.Kind.KNIGHT,
                new Position(7, 1), new Position(5, 2));

        String encoded = Protocol.encode(original);

        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testParseJumpCommand() {
        WireMessage message = Protocol.parse("JUMP BPe4");

        assertEquals(new JumpCommand(Piece.Color.BLACK, Piece.Kind.PAWN, AlgebraicNotation.toPosition("e4")),
                message);
    }

    @Test
    public void testEncodeJumpCommandRoundTrips() {
        JumpCommand original = new JumpCommand(Piece.Color.WHITE, Piece.Kind.ROOK, new Position(4, 4));

        String encoded = Protocol.encode(original);

        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testMoveAcceptedEncodesAsOk() {
        assertEquals("OK", Protocol.encode(new MoveAccepted()));
        assertEquals(new MoveAccepted(), Protocol.parse("OK"));
    }

    @Test
    public void testMoveRejectedRoundTrips() {
        MoveRejected original = new MoveRejected("resting");

        String encoded = Protocol.encode(original);

        assertEquals("REJECT resting", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testParseRejectsEmptyString() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse(""));
    }

    @Test
    public void testParseRejectsGarbage() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("not a real message"));
    }

    @Test
    public void testParseRejectsInvalidColorLetter() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("XQe2e5"));
    }

    @Test
    public void testParseRejectsInvalidKindLetter() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("WXe2e5"));
    }

    @Test
    public void testParseRejectsInvalidSquare() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("WQz9e5"));
    }

    @Test
    public void testParseRejectsJumpWithMissingSquare() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("JUMP WP"));
    }

    @Test
    public void testParseRejectsRejectWithNoReason() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("REJECT "));
    }

    @Test
    public void testParseRejectsRejectWithWhitespaceOnlyReason() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("REJECT  "));
    }

    @Test
    public void testEncodeAndParseBishopMove() {
        MoveCommand original = new MoveCommand(Piece.Color.BLACK, Piece.Kind.BISHOP,
                new Position(0, 2), new Position(3, 5));

        String encoded = Protocol.encode(original);

        assertEquals("BBc8f5", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testEncodeAndParseKingMove() {
        MoveCommand original = new MoveCommand(Piece.Color.WHITE, Piece.Kind.KING,
                new Position(7, 4), new Position(7, 5));

        String encoded = Protocol.encode(original);

        assertEquals("WKe1f1", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testParseRejectsNull() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse(null));
    }

    @Test
    public void testEncodeRejectsNull() {
        assertThrows(MalformedMessageException.class, () -> Protocol.encode(null));
    }
}
