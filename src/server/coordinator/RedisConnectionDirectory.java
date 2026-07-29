package src.server.coordinator;

import redis.clients.jedis.JedisPooled;

import java.util.Optional;

public class RedisConnectionDirectory implements ConnectionDirectory {

    private static final String CONNECTION_KEY_PREFIX = "kongfu:conn:";
    private static final String USER_KEY_PREFIX = "kongfu:user:";
    private static final long ENTRY_TTL_SECONDS = 24L * 60 * 60;

    private final JedisPooled redis;

    public RedisConnectionDirectory(JedisPooled redis) {
        this.redis = redis;
    }

    @Override
    public void assignConnection(String connectionId, String nodeId) {
        redis.setex(CONNECTION_KEY_PREFIX + connectionId, ENTRY_TTL_SECONDS, nodeId);
    }

    @Override
    public Optional<String> nodeForConnection(String connectionId) {
        return Optional.ofNullable(redis.get(CONNECTION_KEY_PREFIX + connectionId));
    }

    @Override
    public void clearConnection(String connectionId) {
        redis.del(CONNECTION_KEY_PREFIX + connectionId);
    }

    @Override
    public void assignUser(String username, String nodeId) {
        redis.setex(USER_KEY_PREFIX + username, ENTRY_TTL_SECONDS, nodeId);
    }

    @Override
    public Optional<String> nodeForUser(String username) {
        return Optional.ofNullable(redis.get(USER_KEY_PREFIX + username));
    }

    @Override
    public void clearUser(String username) {
        redis.del(USER_KEY_PREFIX + username);
    }
}
