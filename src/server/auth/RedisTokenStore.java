package src.server.auth;

import redis.clients.jedis.JedisPooled;

import java.util.Optional;
import java.util.UUID;

public class RedisTokenStore implements TokenStore {

    private static final String TOKEN_KEY_PREFIX = "kongfu:token:";
    private static final long TOKEN_TTL_SECONDS = 24L * 60 * 60;

    private final JedisPooled redis;

    public RedisTokenStore(JedisPooled redis) {
        this.redis = redis;
    }

    @Override
    public String mint(String username) {
        String token = UUID.randomUUID().toString();
        redis.setex(TOKEN_KEY_PREFIX + token, TOKEN_TTL_SECONDS, username);
        return token;
    }

    @Override
    public Optional<String> resolve(String token) {
        return Optional.ofNullable(redis.get(TOKEN_KEY_PREFIX + token));
    }
}
