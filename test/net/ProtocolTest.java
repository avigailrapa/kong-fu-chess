package net;

import org.junit.jupiter.api.Test;
import src.engine.AlgebraicNotation;
import src.model.Piece;
import src.model.Position;
import src.net.JumpCommand;
import src.net.MalformedMessageException;
import src.net.MoveAccepted;
import src.net.MoveCommand;
import src.net.LoginCommand;
import src.net.MoveRejected;
import src.net.Protocol;
import src.net.SelectCommand;
import src.net.Welcome;
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

    @Test
    public void testLoginCommandRoundTrips() {
        LoginCommand original = new LoginCommand("alice");

        String encoded = Protocol.encode(original);

        assertEquals("LOGIN alice", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testParseRejectsLoginWithMissingUsername() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("LOGIN "));
    }

    @Test
    public void testWelcomeRoundTrips() {
        Welcome original = new Welcome(Piece.Color.BLACK);

        String encoded = Protocol.encode(original);

        assertEquals("WELCOME B", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testParseRejectsWelcomeWithInvalidColorLetter() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("WELCOME X"));
    }

    @Test
    public void testSelectCommandWithSquareRoundTrips() {
        SelectCommand original = new SelectCommand(AlgebraicNotation.toPosition("e4"));

        String encoded = Protocol.encode(original);

        assertEquals("SELECT e4", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testSelectCommandWithNoSelectionRoundTrips() {
        SelectCommand original = new SelectCommand(null);

        String encoded = Protocol.encode(original);

        assertEquals("SELECT -", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testParseRejectsSelectWithInvalidSquare() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("SELECT z9"));
    }
}
