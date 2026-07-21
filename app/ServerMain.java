package app;

import src.engine.GameEngine;
import src.io.BoardParser;
import src.model.Board;
import src.server.GameServer;
import src.server.Match;
import src.server.UserStore;

import java.io.File;
import java.net.InetSocketAddress;

public class ServerMain {

    private static final int PORT = 8887;
    private static final long TICK_INTERVAL_MS = 16;
    private static final long BIND_TIMEOUT_MS = 5000;
    private static final String DATA_DIR = "server-data";

    public static void main(String[] args) throws InterruptedException {
        Board board = new BoardParser().parse(BoardParser.STANDARD_STARTING_POSITION);
        GameEngine engine = GameEngine.fromBoard(board);
        Match match = new Match(engine, TICK_INTERVAL_MS);
        new File(DATA_DIR).mkdirs();
        UserStore userStore = new UserStore("jdbc:sqlite:" + DATA_DIR + "/kongfu.db");
        GameServer server = new GameServer(new InetSocketAddress(PORT), match, userStore);

        server.start();
        if (!waitForBoundPort(server)) {
            System.err.println("Server failed to bind to port " + PORT + " within " + BIND_TIMEOUT_MS + "ms");
            System.exit(1);
        }
        System.out.println("KongFu Chess server listening on port " + PORT);
    }

    private static boolean waitForBoundPort(GameServer server) throws InterruptedException {
        long deadline = System.currentTimeMillis() + BIND_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (server.getPort() > 0) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }
}
