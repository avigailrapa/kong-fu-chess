package server.matchmaker;

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
import src.server.allocator.ConnectionDirectory;
import src.server.allocator.InMemoryConnectionDirectory;
import src.server.auth.InMemoryTokenStore;
import src.server.auth.UserStore;
import src.server.core.ClientConnection;
import src.server.matchmaker.Matchmaker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MatchmakerTest {

    private static class FakeConnection implements ClientConnection {
        final List<String> sent = new ArrayList<>();

        @Override
        public void send(String text) {
            sent.add(text);
        }
    }

    private static class RecordingDispatcher implements Matchmaker.MatchDispatcher {
        final List<Object[]> assignCalls = new ArrayList<>();
        final List<Object[]> reconnectCalls = new ArrayList<>();

        @Override
        public void assign(String matchId, List<Matchmaker.PlayerAssignment> players) {
            assignCalls.add(new Object[]{matchId, players});
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

    private Matchmaker newMatchmaker(UserStore store, ConnectionDirectory directory,
                                      Matchmaker.MatchDispatcher dispatcher) {
        connections = new HashMap<>();
        return new Matchmaker(store, new InMemoryTokenStore(), directory, dispatcher,
                connId -> connections.computeIfAbsent(connId, id -> new FakeConnection()));
    }

    private WireMessage lastReply(String connectionId) {
        List<String> sent = connections.get(connectionId).sent;
        return Protocol.parse(sent.get(sent.size() - 1));
    }

    @Test
    public void testLoginCreatesUserAndRepliesWelcome() {
        Matchmaker matchmaker = newMatchmaker(freshStore(), new InMemoryConnectionDirectory(),
                new RecordingDispatcher());

        matchmaker.receive("conn-1", Protocol.encode(new LoginCommand("alice", "secret")));

        assertInstanceOf(Welcome.class, lastReply("conn-1"));
    }

    @Test
    public void testTwoPlayersGetPairedAndDispatchedForAllocation() {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        Matchmaker matchmaker = newMatchmaker(freshStore(), new InMemoryConnectionDirectory(), dispatcher);

        matchmaker.receive("conn-a", Protocol.encode(new LoginCommand("alice", "secret")));
        matchmaker.receive("conn-b", Protocol.encode(new LoginCommand("bob", "secret")));
        matchmaker.receive("conn-a", Protocol.encode(new PlayCommand()));
        matchmaker.receive("conn-b", Protocol.encode(new PlayCommand()));

        assertEquals(1, dispatcher.assignCalls.size());
        Object[] call = dispatcher.assignCalls.get(0);
        @SuppressWarnings("unchecked")
        List<Matchmaker.PlayerAssignment> players = (List<Matchmaker.PlayerAssignment>) call[1];
        assertEquals(2, players.size());
        assertTrue(players.stream().anyMatch(p -> p.username().equals("alice") && p.color() == Piece.Color.WHITE));
        assertTrue(players.stream().anyMatch(p -> p.username().equals("bob") && p.color() == Piece.Color.BLACK));
    }

    @Test
    public void testRoomCreateAndJoinDispatchesMatch() {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        Matchmaker matchmaker = newMatchmaker(freshStore(), new InMemoryConnectionDirectory(), dispatcher);

        matchmaker.receive("conn-a", Protocol.encode(new LoginCommand("alice", "secret")));
        matchmaker.receive("conn-b", Protocol.encode(new LoginCommand("bob", "secret")));
        matchmaker.receive("conn-a", Protocol.encode(new RoomCreateCommand()));
        RoomId roomId = (RoomId) lastReply("conn-a");

        matchmaker.receive("conn-b", Protocol.encode(new RoomJoinCommand(roomId.roomId())));

        assertEquals(1, dispatcher.assignCalls.size());
    }

    @Test
    public void testReconnectDispatchesToOriginalNodeAndSkipsDirectReply() {
        UserStore store = freshStore();
        store.createUser("alice", "secret");
        InMemoryConnectionDirectory directory = new InMemoryConnectionDirectory();
        directory.assignUser("alice", "node-1");
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        Matchmaker matchmaker = newMatchmaker(store, directory, dispatcher);

        matchmaker.receive("conn-new", Protocol.encode(new LoginCommand("alice", "secret")));

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
        Matchmaker matchmaker = newMatchmaker(freshStore(), new InMemoryConnectionDirectory(), dispatcher);

        matchmaker.receive("conn-a", Protocol.encode(new LoginCommand("alice", "secret")));
        matchmaker.receive("conn-a", Protocol.encode(new PlayCommand()));
        matchmaker.disconnect("conn-a");
        matchmaker.receive("conn-b", Protocol.encode(new LoginCommand("bob", "secret")));
        matchmaker.receive("conn-b", Protocol.encode(new PlayCommand()));

        assertEquals(0, dispatcher.assignCalls.size());
    }
}
