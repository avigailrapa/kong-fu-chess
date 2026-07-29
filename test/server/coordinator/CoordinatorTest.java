package server.coordinator;

import org.junit.jupiter.api.Test;
import server.auth.TestDatabase;
import src.model.Piece;
import src.net.Protocol;
import src.net.messages.LoginCommand;
import src.net.messages.PlayCommand;
import src.net.messages.RoomCreateCommand;
import src.net.messages.RoomId;
import src.net.messages.RoomJoinCommand;
import src.net.messages.Welcome;
import src.net.messages.WireMessage;
import src.server.auth.UserStore;
import src.server.core.ClientConnection;
import src.server.coordinator.Allocator;
import src.server.coordinator.ConnectionDirectory;
import src.server.coordinator.Coordinator;
import src.server.coordinator.InMemoryConnectionDirectory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CoordinatorTest {

    private static class FakeConnection implements ClientConnection {
        final List<String> sent = new ArrayList<>();

        @Override
        public void send(String text) {
            sent.add(text);
        }
    }

    private static class RecordingDispatcher implements Coordinator.MatchDispatcher {
        final List<Object[]> createMatchCalls = new ArrayList<>();
        final List<Object[]> reconnectCalls = new ArrayList<>();

        @Override
        public void createMatch(String nodeId, String matchId, List<Coordinator.PlayerAssignment> players) {
            createMatchCalls.add(new Object[]{nodeId, matchId, players});
        }

        @Override
        public void reconnect(String nodeId, String username, String connectionId) {
            reconnectCalls.add(new Object[]{nodeId, username, connectionId});
        }
    }

    private Map<String, FakeConnection> connections;

    private UserStore freshStore() {
        return TestDatabase.freshUserStore();
    }

    private Coordinator newCoordinator(UserStore store, Allocator allocator, ConnectionDirectory directory,
                                        Coordinator.MatchDispatcher dispatcher) {
        connections = new HashMap<>();
        return new Coordinator(store, allocator, directory, dispatcher,
                connId -> connections.computeIfAbsent(connId, id -> new FakeConnection()));
    }

    private WireMessage lastReply(String connectionId) {
        List<String> sent = connections.get(connectionId).sent;
        return Protocol.parse(sent.get(sent.size() - 1));
    }

    @Test
    public void testLoginCreatesUserAndRepliesWelcome() {
        Coordinator coordinator = newCoordinator(freshStore(), new Allocator(List.of("node-1")),
                new InMemoryConnectionDirectory(), new RecordingDispatcher());

        coordinator.receive("conn-1", Protocol.encode(new LoginCommand("alice", "secret")));

        assertInstanceOf(Welcome.class, lastReply("conn-1"));
    }

    @Test
    public void testTwoPlayersGetPairedAndDispatchedToAllocatedNode() {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        Coordinator coordinator = newCoordinator(freshStore(), new Allocator(List.of("node-1", "node-2")),
                new InMemoryConnectionDirectory(), dispatcher);

        coordinator.receive("conn-a", Protocol.encode(new LoginCommand("alice", "secret")));
        coordinator.receive("conn-b", Protocol.encode(new LoginCommand("bob", "secret")));
        coordinator.receive("conn-a", Protocol.encode(new PlayCommand()));
        coordinator.receive("conn-b", Protocol.encode(new PlayCommand()));

        assertEquals(1, dispatcher.createMatchCalls.size());
        Object[] call = dispatcher.createMatchCalls.get(0);
        assertEquals("node-1", call[0]);
        @SuppressWarnings("unchecked")
        List<Coordinator.PlayerAssignment> players = (List<Coordinator.PlayerAssignment>) call[2];
        assertEquals(2, players.size());
        assertTrue(players.stream().anyMatch(p -> p.username().equals("alice") && p.color() == Piece.Color.WHITE));
        assertTrue(players.stream().anyMatch(p -> p.username().equals("bob") && p.color() == Piece.Color.BLACK));
    }

    @Test
    public void testRoomCreateAndJoinDispatchesMatch() {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        Coordinator coordinator = newCoordinator(freshStore(), new Allocator(List.of("node-1")),
                new InMemoryConnectionDirectory(), dispatcher);

        coordinator.receive("conn-a", Protocol.encode(new LoginCommand("alice", "secret")));
        coordinator.receive("conn-b", Protocol.encode(new LoginCommand("bob", "secret")));
        coordinator.receive("conn-a", Protocol.encode(new RoomCreateCommand()));
        RoomId roomId = (RoomId) lastReply("conn-a");

        coordinator.receive("conn-b", Protocol.encode(new RoomJoinCommand(roomId.roomId())));

        assertEquals(1, dispatcher.createMatchCalls.size());
    }

    @Test
    public void testReconnectDispatchesToOriginalNodeAndSkipsDirectReply() {
        UserStore store = freshStore();
        store.createUser("alice", "secret");
        InMemoryConnectionDirectory directory = new InMemoryConnectionDirectory();
        directory.assignUser("alice", "node-1");
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        Coordinator coordinator = newCoordinator(store, new Allocator(List.of("node-1")), directory, dispatcher);

        coordinator.receive("conn-new", Protocol.encode(new LoginCommand("alice", "secret")));

        assertEquals(1, dispatcher.reconnectCalls.size());
        Object[] call = dispatcher.reconnectCalls.get(0);
        assertEquals("node-1", call[0]);
        assertEquals("alice", call[1]);
        assertEquals("conn-new", call[2]);
        assertFalse(connections.containsKey("conn-new"));
    }

    @Test
    public void testDisconnectCancelsMatchmakingSoLeftoverPlayerIsNotPaired() {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        Coordinator coordinator = newCoordinator(freshStore(), new Allocator(List.of("node-1")),
                new InMemoryConnectionDirectory(), dispatcher);

        coordinator.receive("conn-a", Protocol.encode(new LoginCommand("alice", "secret")));
        coordinator.receive("conn-a", Protocol.encode(new PlayCommand()));
        coordinator.disconnect("conn-a");
        coordinator.receive("conn-b", Protocol.encode(new LoginCommand("bob", "secret")));
        coordinator.receive("conn-b", Protocol.encode(new PlayCommand()));

        assertEquals(0, dispatcher.createMatchCalls.size());
    }
}
