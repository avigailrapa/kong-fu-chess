package app;

import io.nats.client.Connection;
import io.nats.client.Nats;
import redis.clients.jedis.JedisPooled;
import src.server.allocator.RedisConnectionDirectory;
import src.server.auth.RedisTokenStore;
import src.server.auth.TokenStore;
import src.server.auth.UserStore;
import src.server.cluster.MatchmakerBridge;
import src.server.cluster.NatsClientConnection;
import src.server.cluster.NatsMatchDispatcher;
import src.server.health.HealthServer;
import src.server.matchmaker.Matchmaker;

import java.net.URI;
import java.util.Map;

public class MatchmakerMain {

    private static final String DEFAULT_NATS_URL = "nats://localhost:4222";
    private static final String DEFAULT_POSTGRES_URL =
            "jdbc:postgresql://localhost:5432/kongfu?user=kongfu&password=kongfu";
    private static final String DEFAULT_REDIS_URL = "redis://localhost:6379";
    private static final String DEFAULT_HEALTH_PORT = "8083";

    public static void main(String[] args) throws Exception {
        String natsUrl = AppSupport.env("NATS_URL", DEFAULT_NATS_URL);
        String postgresUrl = AppSupport.env("POSTGRES_URL", DEFAULT_POSTGRES_URL);
        String redisUrl = AppSupport.env("REDIS_URL", DEFAULT_REDIS_URL);
        int healthPort = Integer.parseInt(AppSupport.env("HEALTH_PORT", DEFAULT_HEALTH_PORT));

        UserStore userStore = new UserStore(postgresUrl);
        JedisPooled redis = new JedisPooled(URI.create(redisUrl));
        Connection nats = Nats.connect(natsUrl);

        RedisConnectionDirectory directory = new RedisConnectionDirectory(redis);
        TokenStore tokenStore = new RedisTokenStore(redis);
        NatsMatchDispatcher dispatcher = new NatsMatchDispatcher(nats);
        Matchmaker matchmaker = new Matchmaker(userStore, tokenStore, directory, dispatcher,
                connId -> new NatsClientConnection(nats, connId));

        new MatchmakerBridge(matchmaker, nats).start();
        new HealthServer(healthPort, () -> Map.of("queuedPlayers", matchmaker.queuedPlayerCount())).start();

        System.out.println("KongFu matchmaker connected to NATS at " + natsUrl);
        Thread.currentThread().join();
    }
}
