package src.server.auth;

import java.util.Optional;

public interface TokenStore {

    String mint(String username);

    Optional<String> resolve(String token);
}
