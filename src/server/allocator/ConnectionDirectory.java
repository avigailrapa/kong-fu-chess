package src.server.allocator;

import java.util.Optional;

public interface ConnectionDirectory {

    void assignConnection(String connectionId, String nodeId);

    Optional<String> nodeForConnection(String connectionId);

    void clearConnection(String connectionId);

    void assignUser(String username, String nodeId);

    Optional<String> nodeForUser(String username);

    void clearUser(String username);
}
