package app;

import io.nats.client.Connection;
import io.nats.client.Nats;
import redis.clients.jedis.JedisPooled;
import src.server.auth.UserStore;
import src.server.cluster.CoordinatorBridge;
import src.server.cluster.NatsClientConnection;
import src.server.cluster.NatsMatchDispatcher;
import src.server.coordinator.Allocator;
import src.server.coordinator.Coordinator;
import src.server.coordinator.RedisConnectionDirectory;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class CoordinatorMain {

    private static final String DEFAULT_NATS_URL = "nats://localhost:4222";
    private static final String DEFAULT_POSTGRES_URL =
            "jdbc:postgresql://localhost:5432/kongfu?user=kongfu&password=kongfu";
    private static final String DEFAULT_REDIS_URL = "redis://localhost:6379";
    private static final String DEFAULT_GAME_NODE_IDS = "node-1";

    public static void main(String[] args) throws Exception {
        String natsUrl = AppSupport.env("NATS_URL", DEFAULT_NATS_URL);
        String postgresUrl = AppSupport.env("POSTGRES_URL", DEFAULT_POSTGRES_URL);
        String redisUrl = AppSupport.env("REDIS_URL", DEFAULT_REDIS_URL);
        List<String> gameNodeIds = Arrays.asList(AppSupport.env("GAME_NODE_IDS", DEFAULT_GAME_NODE_IDS).split(","));

        UserStore userStore = new UserStore(postgresUrl);
        JedisPooled redis = new JedisPooled(URI.create(redisUrl));
        Connection nats = Nats.connect(natsUrl);

        Allocator allocator = new Allocator(gameNodeIds);
        RedisConnectionDirectory directory = new RedisConnectionDirectory(redis);
        NatsMatchDispatcher dispatcher = new NatsMatchDispatcher(nats);
        Coordinator coordinator = new Coordinator(userStore, allocator, directory, dispatcher,
                connId -> new NatsClientConnection(nats, connId));

        new CoordinatorBridge(coordinator, nats).start();

        System.out.println("KongFu coordinator connected to NATS at " + natsUrl + ", nodes: " + gameNodeIds);
        Thread.currentThread().join();
    }
}
