package app;

import src.server.ActivityLog;
import src.server.GameServer;
import src.server.auth.UserStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public class ServerMain {

    private static final long TICK_INTERVAL_MS = 16;
    private static final long BIND_TIMEOUT_MS = 5000;
    private static final long POLL_INTERVAL_MS = 20;
    private static final String CONFIG_FILE = "server.properties";
    private static final int DISCONNECT_COUNTDOWN_SECONDS = 20;

    public static void main(String[] args) throws InterruptedException, IOException {
        Properties config = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            config.load(in);
        }
        int port = Integer.parseInt(config.getProperty("port"));
        String dataDir = config.getProperty("dataDir");
        String databaseFilename = config.getProperty("databaseFilename");

        new File(dataDir).mkdirs();
        UserStore userStore = new UserStore("jdbc:sqlite:" + dataDir + "/" + databaseFilename);
        ActivityLog activityLog = new ActivityLog(dataDir + "/" + ActivityLog.DEFAULT_FILENAME);
        GameServer server = new GameServer(new InetSocketAddress(port), userStore, TICK_INTERVAL_MS,
                DISCONNECT_COUNTDOWN_SECONDS, activityLog);

        server.start();
        if (!waitForBoundPort(server)) {
            System.err.println("Server failed to bind to port " + port + " within " + BIND_TIMEOUT_MS + "ms");
            System.exit(1);
        }
        System.out.println("KongFu Chess server listening on port " + port);
    }

    private static boolean waitForBoundPort(GameServer server) throws InterruptedException {
        long deadline = System.currentTimeMillis() + BIND_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (server.getPort() > 0) {
                return true;
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        return false;
    }
}
