import src.engine.GameEngine;
import src.io.BoardParser;
import src.model.Board;
import src.server.GameServer;
import src.server.Match;

import java.net.InetSocketAddress;

public class ServerMain {

    private static final int PORT = 8887;
    private static final long TICK_INTERVAL_MS = 16;
    private static final long BIND_TIMEOUT_MS = 5000;

    public static void main(String[] args) throws InterruptedException {
        Board board = new BoardParser().parse(BoardParser.STANDARD_STARTING_POSITION);
        GameEngine engine = GameEngine.fromBoard(board);
        Match match = new Match(engine, TICK_INTERVAL_MS);
        GameServer server = new GameServer(new InetSocketAddress(PORT), match);

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
