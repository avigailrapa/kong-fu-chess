package app;

import io.nats.client.Connection;
import io.nats.client.Nats;
import redis.clients.jedis.JedisPooled;
import src.server.allocator.Allocator;
import src.server.allocator.GameAllocator;
import src.server.allocator.RedisConnectionDirectory;
import src.server.health.HealthServer;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GameAllocatorMain {

    private static final String DEFAULT_NATS_URL = "nats://localhost:4222";
    private static final String DEFAULT_REDIS_URL = "redis://localhost:6379";
    private static final String DEFAULT_GAME_NODE_IDS = "node-1";
    private static final String DEFAULT_HEALTH_PORT = "8084";

    public static void main(String[] args) throws Exception {
        String natsUrl = AppSupport.env("NATS_URL", DEFAULT_NATS_URL);
        String redisUrl = AppSupport.env("REDIS_URL", DEFAULT_REDIS_URL);
        List<String> gameNodeIds = Arrays.asList(AppSupport.env("GAME_NODE_IDS", DEFAULT_GAME_NODE_IDS).split(","));
        int healthPort = Integer.parseInt(AppSupport.env("HEALTH_PORT", DEFAULT_HEALTH_PORT));

        JedisPooled redis = new JedisPooled(URI.create(redisUrl));
        Connection nats = Nats.connect(natsUrl);

        Allocator allocator = new Allocator(gameNodeIds);
        RedisConnectionDirectory directory = new RedisConnectionDirectory(redis);
        GameAllocator gameAllocator = new GameAllocator(allocator, directory, nats);
        gameAllocator.start();

        new HealthServer(healthPort, () -> Map.of("matchesAllocated", gameAllocator.matchesAllocated())).start();

        System.out.println("KongFu game-allocator connected to NATS at " + natsUrl + ", nodes: " + gameNodeIds);
        Thread.currentThread().join();
    }
}
