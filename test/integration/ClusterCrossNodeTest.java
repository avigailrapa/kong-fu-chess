package integration;

import io.nats.client.Connection;
import io.nats.client.Nats;
import org.junit.jupiter.api.Test;
import server.auth.TestDatabase;
import server.cluster.NatsServerProcess;
import src.net.MalformedMessageException;
import src.net.Protocol;
import src.net.messages.LoginCommand;
import src.net.messages.MatchFound;
import src.net.messages.MoveRejected;
import src.net.messages.NewGameCommand;
import src.net.messages.PlayCommand;
import src.net.messages.WireMessage;
import src.server.Lobby;
import src.server.allocator.Allocator;
import src.server.allocator.GameAllocator;
import src.server.allocator.InMemoryConnectionDirectory;
import src.server.auth.InMemoryTokenStore;
import src.server.auth.UserStore;
import src.server.cluster.ClusterProtocol;
import src.server.cluster.GameNodeBridge;
import src.server.cluster.MatchmakerBridge;
import src.server.cluster.NatsClientConnection;
import src.server.cluster.NatsMatchDispatcher;
import src.server.history.GameStore;
import src.server.matchmaker.Matchmaker;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ClusterCrossNodeTest {

    @Test
    public void testMatchmakerAndGameAllocatorPairPlayersAndRouteMessagesAcrossGameNodeShards() throws Exception {
        NatsServerProcess natsServer = NatsServerProcess.start();
        Connection nats = Nats.connect(natsServer.url());
        try {
            UserStore matchmakerStore = TestDatabase.freshUserStore();
            UserStore nodeStore = new UserStore(TestDatabase.JDBC_URL);
            GameStore nodeGameStore = TestDatabase.freshGameStore();

            InMemoryConnectionDirectory directory = new InMemoryConnectionDirectory();
            Matchmaker matchmaker = new Matchmaker(matchmakerStore, new InMemoryTokenStore(), directory,
                    new NatsMatchDispatcher(nats), connId -> new NatsClientConnection(nats, connId));
            new MatchmakerBridge(matchmaker, nats).start();

            Allocator allocator = new Allocator(List.of("node-1", "node-2"));
            new GameAllocator(allocator, directory, nats).start();

            Lobby lobbyNode1 = new Lobby(nodeStore, nodeGameStore, 100, 20,
                    connId -> new NatsClientConnection(nats, (String) connId));
            new GameNodeBridge(lobbyNode1, nats, "node-1").start();
            Lobby lobbyNode2 = new Lobby(nodeStore, nodeGameStore, 100, 20,
                    connId -> new NatsClientConnection(nats, (String) connId));
            new GameNodeBridge(lobbyNode2, nats, "node-2").start();

            Map<String, BlockingQueue<String>> outboxes = new ConcurrentHashMap<>();
            String aliceConn = subscribeOutbox(nats, outboxes, "alice-conn");
            String bobConn = subscribeOutbox(nats, outboxes, "bob-conn");
            String carolConn = subscribeOutbox(nats, outboxes, "carol-conn");
            String daveConn = subscribeOutbox(nats, outboxes, "dave-conn");

            login(nats, aliceConn, "alice");
            login(nats, bobConn, "bob");
            login(nats, carolConn, "carol");
            login(nats, daveConn, "dave");

            play(nats, aliceConn);
            play(nats, bobConn);
            play(nats, carolConn);
            play(nats, daveConn);

            MatchFound aliceMatch = awaitMessage(outboxes, aliceConn, MatchFound.class);
            MatchFound carolMatch = awaitMessage(outboxes, carolConn, MatchFound.class);
            assertEquals("bob", aliceMatch.opponentUsername());
            assertEquals("dave", carolMatch.opponentUsername());

            assertNotNull(lobbyNode1.matchFor(aliceConn), "alice vs bob should be hosted on node-1 (allocator's first pick)");
            assertNull(lobbyNode2.matchFor(aliceConn), "alice's match must not also exist on node-2");
            assertNotNull(lobbyNode2.matchFor(carolConn), "carol vs dave should be hosted on node-2 (allocator's second pick)");
            assertNull(lobbyNode1.matchFor(carolConn), "carol's match must not also exist on node-1");

            nats.publish(ClusterProtocol.nodeInboundSubject("node-1"),
                    ClusterProtocol.encodeInbound(aliceConn, Protocol.encode(new NewGameCommand())));
            MoveRejected rejected = awaitMessage(outboxes, aliceConn, MoveRejected.class);
            assertEquals("game_in_progress", rejected.reason());
        } finally {
            nats.close();
            natsServer.stop();
        }
    }

    private String subscribeOutbox(Connection nats, Map<String, BlockingQueue<String>> outboxes, String connectionId) {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        outboxes.put(connectionId, queue);
        nats.createDispatcher(message -> queue.add(new String(message.getData(), java.nio.charset.StandardCharsets.UTF_8)))
                .subscribe(ClusterProtocol.outboundSubject(connectionId));
        return connectionId;
    }

    private void login(Connection nats, String connectionId, String username) {
        nats.publish(ClusterProtocol.LOBBY_INBOUND_SUBJECT,
                ClusterProtocol.encodeInbound(connectionId, Protocol.encode(new LoginCommand(username, "pw"))));
    }

    private void play(Connection nats, String connectionId) {
        nats.publish(ClusterProtocol.LOBBY_INBOUND_SUBJECT,
                ClusterProtocol.encodeInbound(connectionId, Protocol.encode(new PlayCommand())));
    }

    private <T extends WireMessage> T awaitMessage(Map<String, BlockingQueue<String>> outboxes, String connectionId,
                                                     Class<T> expectedType) throws InterruptedException {
        long deadlineMs = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadlineMs) {
            String raw = outboxes.get(connectionId).poll(200, TimeUnit.MILLISECONDS);
            if (raw == null) {
                continue;
            }
            WireMessage parsed;
            try {
                parsed = Protocol.parse(raw);
            } catch (MalformedMessageException e) {
                continue;
            }
            if (expectedType.isInstance(parsed)) {
                return expectedType.cast(parsed);
            }
        }
        throw new AssertionError("timed out waiting for a " + expectedType.getSimpleName()
                + " on connection " + connectionId);
    }
}
