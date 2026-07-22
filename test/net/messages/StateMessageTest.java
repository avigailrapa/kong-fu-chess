package net.messages;

import org.junit.jupiter.api.Test;
import src.model.Piece;
import src.model.Position;
import src.net.MalformedMessageException;
import src.net.Protocol;
import src.net.messages.StateMessage;
import src.net.messages.WireMessage;
import src.view.GameSnapshot;
import src.view.PieceSnapshot;
import src.view.SelectionSnapshot;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class StateMessageTest {

    private GameSnapshot fullSnapshot() {
        PieceSnapshot[][] board = new PieceSnapshot[3][3];
        board[0][0] = new PieceSnapshot("p0", Piece.Color.WHITE, Piece.Kind.ROOK, Piece.State.IDLE,
                10, 20, 500L, 0L);
        board[2][1] = new PieceSnapshot("p1", Piece.Color.BLACK, Piece.Kind.KNIGHT, Piece.State.SHORT_REST,
                40, 80, 250L, 1000L);
        List<SelectionSnapshot> selections = List.of(new SelectionSnapshot(Piece.Color.WHITE, new Position(0, 0)));
        Set<Position> legalDestinations = Set.of(new Position(1, 0), new Position(0, 1));
        List<String> whiteMoveLog = List.of("00:01.000 Ra3");
        List<String> blackMoveLog = List.of("00:02.500 Nc6", "00:04.000 Nxe5");
        return new GameSnapshot(3, 3, board, selections, legalDestinations, false, null, 3, 9,
                whiteMoveLog, blackMoveLog, 1.25);
    }

    private void assertSnapshotsEqual(GameSnapshot expected, GameSnapshot actual) {
        assertEquals(expected.width(), actual.width());
        assertEquals(expected.height(), actual.height());
        assertEquals(expected.gameOver(), actual.gameOver());
        assertEquals(expected.winner(), actual.winner());
        assertEquals(expected.whiteScore(), actual.whiteScore());
        assertEquals(expected.blackScore(), actual.blackScore());
        assertEquals(expected.whiteMoveLog(), actual.whiteMoveLog());
        assertEquals(expected.blackMoveLog(), actual.blackMoveLog());
        assertEquals(expected.zoom(), actual.zoom());
        assertEquals(expected.selections(), actual.selections());
        assertEquals(expected.legalDestinations(), actual.legalDestinations());
        for (int row = 0; row < expected.height(); row++) {
            for (int col = 0; col < expected.width(); col++) {
                Position position = new Position(row, col);
                assertEquals(expected.pieceAt(position), actual.pieceAt(position),
                        "mismatch at " + position);
            }
        }
    }

    @Test
    public void testFullSnapshotRoundTrips() {
        GameSnapshot original = fullSnapshot();

        String encoded = Protocol.encode(new StateMessage(original));
        WireMessage decoded = Protocol.parse(encoded);

        assertInstanceOf(StateMessage.class, decoded);
        assertSnapshotsEqual(original, ((StateMessage) decoded).snapshot());
    }

    @Test
    public void testGameOverWithWinnerRoundTrips() {
        GameSnapshot original = new GameSnapshot(3, 3, new PieceSnapshot[3][3], List.of(), Set.of(),
                true, Piece.Color.BLACK, 12, 4, List.of(), List.of(), 1.0);

        String encoded = Protocol.encode(new StateMessage(original));
        GameSnapshot decoded = ((StateMessage) Protocol.parse(encoded)).snapshot();

        assertSnapshotsEqual(original, decoded);
    }

    @Test
    public void testEmptyBoardNoSelectionsNoLegalMovesNoLogsRoundTrips() {
        GameSnapshot original = new GameSnapshot(2, 2, new PieceSnapshot[2][2], List.of(), Set.of(),
                false, null, 0, 0, List.of(), List.of(), GameSnapshot.DEFAULT_ZOOM);

        String encoded = Protocol.encode(new StateMessage(original));
        GameSnapshot decoded = ((StateMessage) Protocol.parse(encoded)).snapshot();

        assertSnapshotsEqual(original, decoded);
    }

    @Test
    public void testMultipleSelectionsRoundTrip() {
        List<SelectionSnapshot> selections = List.of(
                new SelectionSnapshot(Piece.Color.WHITE, new Position(0, 0)),
                new SelectionSnapshot(Piece.Color.BLACK, new Position(2, 1)));
        GameSnapshot original = new GameSnapshot(3, 3, new PieceSnapshot[3][3], selections, Set.of(),
                false, null, 0, 0, List.of(), List.of(), 1.0);

        String encoded = Protocol.encode(new StateMessage(original));
        GameSnapshot decoded = ((StateMessage) Protocol.parse(encoded)).snapshot();

        assertSnapshotsEqual(original, decoded);
    }

    @Test
    public void testParseRejectsMalformedStateHeader() {
        assertThrows(MalformedMessageException.class, () -> Protocol.parse("STATE not enough fields"));
    }

    @Test
    public void testParseRejectsUnrecognizedLineWithinStateBlock() {
        String malformed = "STATE 1 1 0 - 0 0 1.0\nNONSENSE 1 2 3\nENDSTATE";

        assertThrows(MalformedMessageException.class, () -> Protocol.parse(malformed));
    }

    @Test
    public void testParseRejectsStateBlockMissingEndState() {
        String malformed = "STATE 1 1 0 - 0 0 1.0";

        assertThrows(MalformedMessageException.class, () -> Protocol.parse(malformed));
    }
}
