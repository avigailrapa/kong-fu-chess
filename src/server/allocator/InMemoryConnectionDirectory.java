package src.server.allocator;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryConnectionDirectory implements ConnectionDirectory {

    private final Map<String, String> nodeByConnection = new ConcurrentHashMap<>();
    private final Map<String, String> nodeByUser = new ConcurrentHashMap<>();

    @Override
    public void assignConnection(String connectionId, String nodeId) {
        nodeByConnection.put(connectionId, nodeId);
    }

    @Override
    public Optional<String> nodeForConnection(String connectionId) {
        return Optional.ofNullable(nodeByConnection.get(connectionId));
    }

    @Override
    public void clearConnection(String connectionId) {
        nodeByConnection.remove(connectionId);
    }

    @Override
    public void assignUser(String username, String nodeId) {
        nodeByUser.put(username, nodeId);
    }

    @Override
    public Optional<String> nodeForUser(String username) {
        return Optional.ofNullable(nodeByUser.get(username));
    }

    @Override
    public void clearUser(String username) {
        nodeByUser.remove(username);
    }
}
