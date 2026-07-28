package app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public static void main(String[] args) throws InterruptedException {
        Properties config = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            config.load(in);
        } catch (IOException e) {
            System.err.println("Could not read " + CONFIG_FILE + ": " + e.getMessage());
            System.exit(1);
            return;
        }
        int port = Integer.parseInt(requireProperty(config, "port"));
        String dataDir = requireProperty(config, "dataDir");
        String databaseFilename = requireProperty(config, "databaseFilename");

        new File(dataDir).mkdirs();
        System.setProperty("LOG_FILE", dataDir + "/server.log");
        Logger log = LoggerFactory.getLogger(ServerMain.class);

        UserStore userStore = new UserStore("jdbc:sqlite:" + dataDir + "/" + databaseFilename);
        GameServer server = new GameServer(new InetSocketAddress(port), userStore, TICK_INTERVAL_MS,
                DISCONNECT_COUNTDOWN_SECONDS);

        server.start();
        if (!waitForBoundPort(server)) {
            log.error("Server failed to bind to port {} within {}ms", port, BIND_TIMEOUT_MS);
            System.exit(1);
        }
        log.info("KongFu Chess server listening on port {}", port);
    }

    private static String requireProperty(Properties config, String key) {
        String value = config.getProperty(key);
        if (value == null) {
            System.err.println(CONFIG_FILE + " is missing required property \"" + key + "\"");
            System.exit(1);
        }
        return value;
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
