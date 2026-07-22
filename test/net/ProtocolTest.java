package net;

import org.junit.jupiter.api.Test;
import src.engine.AlgebraicNotation;
import src.engine.GameOverEvent;
import src.engine.MoveEvent;
import src.model.Piece;
import src.model.Position;
import src.net.MalformedMessageException;
import src.net.Protocol;
import src.net.messages.CancelPlayCommand;
import src.net.messages.DisconnectCountdown;
import src.net.messages.GameOverMessage;
import src.net.messages.JumpCommand;
import src.net.messages.MatchFound;
import src.net.messages.MatchTimeout;
import src.net.messages.MoveAccepted;
import src.net.messages.MoveCommand;
import src.net.messages.MoveOccurred;
import src.net.messages.LoginCommand;
import src.net.messages.MoveRejected;
import src.net.messages.NewGameCommand;
import src.net.messages.PlayCommand;
import src.net.messages.RatingChanged;
import src.net.messages.RoomCreateCommand;
import src.net.messages.RoomId;
import src.net.messages.RoomJoinCommand;
import src.net.messages.SelectCommand;
import src.net.messages.Spectating;
import src.net.messages.Welcome;
import src.net.messages.WireMessage;

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
        LoginCommand original = new LoginCommand("alice", "secret");

        String encoded = Protocol.encode(original);

        assertEquals("LOGIN alice secret", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testParseRejectsLoginWithMissingUsername() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("LOGIN "));
    }

    @Test
    public void testParseRejectsLoginWithMissingPassword() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("LOGIN alice"));
    }

    @Test
    public void testWelcomeRoundTrips() {
        Welcome original = new Welcome(1200);

        String encoded = Protocol.encode(original);

        assertEquals("WELCOME 1200", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testParseRejectsWelcomeWithNonNumericRating() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("WELCOME abc"));
    }

    @Test
    public void testParseRejectsWelcomeWithMissingRating() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("WELCOME"));
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

    @Test
    public void testMoveOccurredRoundTrips() {
        MoveOccurred original = new MoveOccurred(new MoveEvent(Piece.Color.WHITE, Piece.Kind.PAWN,
                AlgebraicNotation.toPosition("e2"), AlgebraicNotation.toPosition("e4"), true, false, false, 1234));

        String encoded = Protocol.encode(original);

        assertEquals("EVENT_MOVE WPe2e4 1 0 0 1234", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testMoveOccurredWithKingCaptureRoundTrips() {
        MoveOccurred original = new MoveOccurred(new MoveEvent(Piece.Color.BLACK, Piece.Kind.QUEEN,
                AlgebraicNotation.toPosition("d8"), AlgebraicNotation.toPosition("d1"), true, true, false, 5000));

        String encoded = Protocol.encode(original);

        assertEquals("EVENT_MOVE BQd8d1 1 1 0 5000", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testMoveOccurredWithPromotionRoundTrips() {
        MoveOccurred original = new MoveOccurred(new MoveEvent(Piece.Color.WHITE, Piece.Kind.QUEEN,
                AlgebraicNotation.toPosition("e7"), AlgebraicNotation.toPosition("e8"), false, false, true, 4321));

        String encoded = Protocol.encode(original);

        assertEquals("EVENT_MOVE WQe7e8 0 0 1 4321", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testParseRejectsMalformedMoveEvent() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("EVENT_MOVE WPe2e4 2 0 0 1234"));
    }

    @Test
    public void testGameOverMessageRoundTrips() {
        GameOverMessage original = new GameOverMessage(new GameOverEvent(Piece.Color.WHITE));

        String encoded = Protocol.encode(original);

        assertEquals("EVENT_GAMEOVER W", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testParseRejectsGameOverWithInvalidColorLetter() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("EVENT_GAMEOVER X"));
    }

    @Test
    public void testNewGameCommandRoundTrips() {
        NewGameCommand original = new NewGameCommand();

        String encoded = Protocol.encode(original);

        assertEquals("NEWGAME", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testRatingChangedRoundTrips() {
        RatingChanged original = new RatingChanged(1234);

        String encoded = Protocol.encode(original);

        assertEquals("RATING 1234", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testParseRejectsMalformedRating() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("RATING abc"));
    }

    @Test
    public void testPlayCommandRoundTrips() {
        PlayCommand original = new PlayCommand();

        String encoded = Protocol.encode(original);

        assertEquals("PLAY", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testCancelPlayCommandRoundTrips() {
        CancelPlayCommand original = new CancelPlayCommand();

        String encoded = Protocol.encode(original);

        assertEquals("CANCEL_PLAY", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testMatchFoundRoundTrips() {
        MatchFound original = new MatchFound("bob", Piece.Color.WHITE, 1250);

        String encoded = Protocol.encode(original);

        assertEquals("MATCH_FOUND bob W 1250", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testParseRejectsMalformedMatchFound() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("MATCH_FOUND bob X 1250"));
    }

    @Test
    public void testMatchTimeoutRoundTrips() {
        MatchTimeout original = new MatchTimeout();

        String encoded = Protocol.encode(original);

        assertEquals("MATCH_TIMEOUT", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testDisconnectCountdownRoundTrips() {
        DisconnectCountdown original = new DisconnectCountdown(15);

        String encoded = Protocol.encode(original);

        assertEquals("DISCONNECT_COUNTDOWN 15", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testParseRejectsMalformedDisconnectCountdown() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("DISCONNECT_COUNTDOWN abc"));
    }

    @Test
    public void testRoomCreateCommandRoundTrips() {
        RoomCreateCommand original = new RoomCreateCommand();

        String encoded = Protocol.encode(original);

        assertEquals("ROOM_CREATE", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testRoomJoinCommandRoundTrips() {
        RoomJoinCommand original = new RoomJoinCommand("AB12CD");

        String encoded = Protocol.encode(original);

        assertEquals("ROOM_JOIN AB12CD", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testRoomIdRoundTrips() {
        RoomId original = new RoomId("AB12CD");

        String encoded = Protocol.encode(original);

        assertEquals("ROOM_ID AB12CD", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }

    @Test
    public void testSpectatingRoundTrips() {
        Spectating original = new Spectating();

        String encoded = Protocol.encode(original);

        assertEquals("SPECTATING", encoded);
        assertEquals(original, Protocol.parse(encoded));
    }
}
