package app;

import io.nats.client.Connection;
import io.nats.client.Nats;
import redis.clients.jedis.JedisPooled;
import src.server.cluster.WsGateway;
import src.server.allocator.RedisConnectionDirectory;
import src.server.health.HealthServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

public class WsGatewayMain {

    static final int DEFAULT_PORT = 8887;
    private static final String DEFAULT_NATS_URL = "nats://localhost:4222";
    private static final String DEFAULT_REDIS_URL = "redis://localhost:6379";
    private static final String DEFAULT_HEALTH_PORT = "8081";
    private static final long BIND_TIMEOUT_MS = 5000;
    private static final long POLL_INTERVAL_MS = 20;

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(AppSupport.env("WS_PORT", String.valueOf(DEFAULT_PORT)));
        String natsUrl = AppSupport.env("NATS_URL", DEFAULT_NATS_URL);
        String redisUrl = AppSupport.env("REDIS_URL", DEFAULT_REDIS_URL);
        int healthPort = Integer.parseInt(AppSupport.env("HEALTH_PORT", DEFAULT_HEALTH_PORT));

        Connection nats = Nats.connect(natsUrl);
        JedisPooled redis = new JedisPooled(URI.create(redisUrl));
        WsGateway gateway = new WsGateway(new InetSocketAddress(port), nats, new RedisConnectionDirectory(redis));
        gateway.start();
        if (!waitForBoundPort(gateway)) {
            System.err.println("ws-gateway failed to bind to port " + port + " within " + BIND_TIMEOUT_MS + "ms");
            System.exit(1);
        }
        new HealthServer(healthPort, () -> Map.of("connections", gateway.connectionCount())).start();
        System.out.println("KongFu ws-gateway listening on port " + port + ", relaying to NATS at " + natsUrl);
    }

    private static boolean waitForBoundPort(WsGateway gateway) throws InterruptedException {
        long deadline = System.currentTimeMillis() + BIND_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (gateway.getPort() > 0) {
                return true;
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        return false;
    }
}
