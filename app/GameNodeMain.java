package app;

import io.nats.client.Connection;
import io.nats.client.Nats;
import src.server.Lobby;
import src.server.auth.UserStore;
import src.server.cluster.GameNodeBridge;
import src.server.cluster.NatsClientConnection;
import src.server.health.HealthServer;
import src.server.history.GameStore;

import java.io.File;
import java.util.Map;

public class GameNodeMain {

    private static final long DEFAULT_TICK_INTERVAL_MS = 16;
    private static final int DEFAULT_DISCONNECT_COUNTDOWN_SECONDS = 20;
    private static final String DEFAULT_NATS_URL = "nats://localhost:4222";
    private static final String DEFAULT_POSTGRES_URL =
            "jdbc:postgresql://localhost:5432/kongfu?user=kongfu&password=kongfu";
    private static final String DEFAULT_DATA_DIR = "data/server";
    private static final String DEFAULT_NODE_ID = "node-1";
    private static final String DEFAULT_HEALTH_PORT = "8082";

    public static void main(String[] args) throws Exception {
        String natsUrl = AppSupport.env("NATS_URL", DEFAULT_NATS_URL);
        String postgresUrl = AppSupport.env("POSTGRES_URL", DEFAULT_POSTGRES_URL);
        String dataDir = AppSupport.env("DATA_DIR", DEFAULT_DATA_DIR);
        String nodeId = AppSupport.env("NODE_ID", DEFAULT_NODE_ID);
        long tickIntervalMs = Long.parseLong(
                AppSupport.env("TICK_INTERVAL_MS", String.valueOf(DEFAULT_TICK_INTERVAL_MS)));
        int disconnectCountdownSeconds = Integer.parseInt(
                AppSupport.env("DISCONNECT_COUNTDOWN_SECONDS", String.valueOf(DEFAULT_DISCONNECT_COUNTDOWN_SECONDS)));
        int healthPort = Integer.parseInt(AppSupport.env("HEALTH_PORT", DEFAULT_HEALTH_PORT));

        new File(dataDir).mkdirs();
        System.setProperty("LOG_FILE", dataDir + "/game-node.log");
        UserStore userStore = new UserStore(postgresUrl);
        GameStore gameStore = new GameStore(postgresUrl);

        Connection nats = Nats.connect(natsUrl);
        Lobby lobby = new Lobby(userStore, gameStore, tickIntervalMs, disconnectCountdownSeconds,
                connId -> new NatsClientConnection(nats, (String) connId));
        new GameNodeBridge(lobby, nats, nodeId).start();
        new HealthServer(healthPort, () -> Map.of("activeMatches", lobby.activeMatchCount())).start();

        System.out.println("KongFu game-node " + nodeId + " connected to NATS at " + natsUrl);
        Thread.currentThread().join();
    }
}
