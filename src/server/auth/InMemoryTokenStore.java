package src.server.auth;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTokenStore implements TokenStore {

    private final Map<String, String> usernameByToken = new ConcurrentHashMap<>();

    @Override
    public String mint(String username) {
        String token = UUID.randomUUID().toString();
        usernameByToken.put(token, username);
        return token;
    }

    @Override
    public Optional<String> resolve(String token) {
        return Optional.ofNullable(usernameByToken.get(token));
    }
}
