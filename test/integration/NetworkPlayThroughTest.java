package integration;

import org.junit.jupiter.api.Test;
import src.engine.GameEngine;
import src.engine.MoveResult;
import src.model.Board;
import src.model.GameState;
import src.model.Piece;
import src.model.Position;
import src.net.NetworkGameProxy;
import src.realtime.RealTimeArbiter;
import src.rules.PieceRules;
import src.rules.RookRule;
import src.rules.RuleEngine;
import src.server.GameServer;
import src.server.Match;
import src.view.GameSnapshot;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkPlayThroughTest {

    @Test
    public void testMoveRequestedThroughProxyIsAcceptedAndSnapshotUpdates() throws Exception {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0)), new Position(7, 0));
        Map<Piece.Kind, PieceRules> rulesByKind = Map.of(Piece.Kind.ROOK, new RookRule());
        GameEngine engine = new GameEngine(board, new GameState(), new RuleEngine(rulesByKind),
                new RealTimeArbiter(board));
        Match match = new Match(engine, 100);
        GameServer server = new GameServer(new InetSocketAddress("localhost", 0), match);
        server.start();

        int port = waitForBoundPort(server);
        NetworkGameProxy proxy = new NetworkGameProxy(URI.create("ws://localhost:" + port), 5000);
        try {
            assertTrue(proxy.connectBlocking(5, TimeUnit.SECONDS), "expected the client to connect");

            waitUntil(() -> proxy.latestSnapshot() != null, "an initial STATE broadcast");
            assertTrue(proxy.isOccupied(new Position(7, 0)));

            MoveResult result = proxy.requestMove(new Position(7, 0), new Position(4, 0));

            assertTrue(result.isAccepted(), "expected the move to be accepted: " + result.reason());
            waitUntil(() -> pieceHasArrived(proxy, new Position(4, 0)), "the piece to arrive at its destination");
        } finally {
            proxy.closeBlocking();
            match.stop();
            server.stop();
        }
    }

    private boolean pieceHasArrived(NetworkGameProxy proxy, Position destination) {
        GameSnapshot snapshot = proxy.latestSnapshot();
        return snapshot != null && snapshot.pieceAt(destination) != null;
    }

    private int waitForBoundPort(GameServer server) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            int port = server.getPort();
            if (port > 0) {
                return port;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("server did not bind a port in time");
    }

    private void waitUntil(java.util.function.BooleanSupplier condition, String descriptionOfWhatWeWaitedFor)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("timed out waiting for " + descriptionOfWhatWeWaitedFor);
    }
}
