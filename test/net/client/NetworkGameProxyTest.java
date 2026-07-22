package net.client;

import org.junit.jupiter.api.Test;
import src.engine.MoveResult;
import src.model.Piece;
import src.model.Position;
import src.net.Protocol;
import src.net.client.NetworkGameProxy;
import src.net.messages.RatingChanged;
import src.net.messages.StateMessage;
import src.view.GameSnapshot;
import src.view.PieceSnapshot;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkGameProxyTest {

    private NetworkGameProxy newProxy() {
        return new NetworkGameProxy(URI.create("ws://localhost:1"), 200);
    }

    @Test
    public void testIsOccupiedFalseBeforeAnyStateReceived() {
        NetworkGameProxy proxy = newProxy();

        assertFalse(proxy.isOccupied(new Position(0, 0)));
    }

    @Test
    public void testLatestSnapshotNullBeforeAnyStateReceived() {
        NetworkGameProxy proxy = newProxy();

        assertNull(proxy.latestSnapshot());
    }

    @Test
    public void testIsOccupiedReflectsMostRecentState() {
        NetworkGameProxy proxy = newProxy();
        PieceSnapshot[][] board = new PieceSnapshot[8][8];
        board[7][0] = new PieceSnapshot("p0", Piece.Color.WHITE, Piece.Kind.ROOK, Piece.State.IDLE, 0, 0, 0L, 0L);
        GameSnapshot snapshot = new GameSnapshot(8, 8, board, List.of(), Set.of(), false, null, 0, 0,
                List.of(), List.of(), 1.0);

        proxy.onMessage(Protocol.encode(new StateMessage(snapshot)));

        assertTrue(proxy.isOccupied(new Position(7, 0)));
        assertFalse(proxy.isOccupied(new Position(0, 0)));
    }

    @Test
    public void testLatestSnapshotReflectsMostRecentState() {
        NetworkGameProxy proxy = newProxy();
        GameSnapshot snapshot = new GameSnapshot(8, 8, new PieceSnapshot[8][8], List.of(), Set.of(),
                true, Piece.Color.BLACK, 5, 9, List.of(), List.of(), 1.0);

        proxy.onMessage(Protocol.encode(new StateMessage(snapshot)));

        assertNotNull(proxy.latestSnapshot());
        assertTrue(proxy.latestSnapshot().gameOver());
        assertEquals(Piece.Color.BLACK, proxy.latestSnapshot().winner());
    }

    @Test
    public void testMalformedIncomingMessageIsIgnored() {
        NetworkGameProxy proxy = newProxy();

        assertDoesNotThrow(() -> proxy.onMessage("not a real message"));
        assertNull(proxy.latestSnapshot());
    }

    @Test
    public void testRequestMoveFromEmptySourceReturnsEmptySourceWithoutSending() {
        NetworkGameProxy proxy = newProxy();
        GameSnapshot snapshot = new GameSnapshot(8, 8, new PieceSnapshot[8][8], List.of(), Set.of(), false, null,
                0, 0, List.of(), List.of(), 1.0);
        proxy.onMessage(Protocol.encode(new StateMessage(snapshot)));

        MoveResult result = proxy.requestMove(new Position(0, 0), new Position(1, 0));

        assertFalse(result.isAccepted());
        assertEquals("empty_source", result.reason());
    }

    @Test
    public void testRequestMoveBeforeAnyStateReturnsEmptySourceWithoutSending() {
        NetworkGameProxy proxy = newProxy();

        MoveResult result = proxy.requestMove(new Position(0, 0), new Position(1, 0));

        assertFalse(result.isAccepted());
        assertEquals("empty_source", result.reason());
    }

    @Test
    public void testRequestJumpFromEmptySquareDoesNothingWithoutSending() {
        NetworkGameProxy proxy = newProxy();

        assertDoesNotThrow(() -> proxy.requestJump(new Position(0, 0)));
    }

    @Test
    public void testRatingChangedUpdatesLatestRating() {
        NetworkGameProxy proxy = newProxy();

        proxy.onMessage(Protocol.encode(new RatingChanged(1234)));

        assertEquals(1234, proxy.latestRating());
    }
}
