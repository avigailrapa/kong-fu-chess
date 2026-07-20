import src.bus.EventBus;
import src.input.BoardMapper;
import src.input.Controller;
import src.net.NetworkGameProxy;
import src.view.GameSnapshot;
import src.view.GameWindow;
import src.view.Renderer;
import src.view.sound.ClipSoundPlayer;
import src.view.sound.EffectsController;

import javax.swing.*;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleFunction;
import java.util.function.LongPredicate;
import java.util.function.Supplier;

public class ClientMain {

    private static final String DEFAULT_SERVER_URL = "ws://localhost:8887";
    private static final long REQUEST_TIMEOUT_MS = 2000;
    private static final long CONNECT_TIMEOUT_SECONDS = 5;
    private static final long INITIAL_STATE_TIMEOUT_MS = 5000;
    private static final int BOARD_WIDTH = 8;
    private static final int BOARD_HEIGHT = 8;

    public static void main(String[] args) throws Exception {
        String serverUrl = args.length > 0 ? args[0] : DEFAULT_SERVER_URL;

        NetworkGameProxy proxy = new NetworkGameProxy(URI.create(serverUrl), REQUEST_TIMEOUT_MS);
        try {
            if (!proxy.connectBlocking(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                System.err.println("Could not connect to server at " + serverUrl);
                proxy.close();
                System.exit(1);
            }
            waitForInitialState(proxy);
        } catch (Exception e) {
            System.err.println("Failed to connect to server at " + serverUrl + ": " + e.getMessage());
            proxy.close();
            System.exit(1);
        }

        System.setProperty("sun.java2d.uiScale", "1");
        System.setProperty("sun.java2d.dpiaware", "true");

        Supplier<GameWindow.GameComponents> gameFactory = () -> createGame(proxy);
        GameWindow window = new GameWindow(gameFactory);

        SwingUtilities.invokeLater(window::open);
    }

    private static GameWindow.GameComponents createGame(NetworkGameProxy proxy) {
        Controller controller = new Controller(new BoardMapper(BOARD_WIDTH, BOARD_HEIGHT), proxy);
        Renderer renderer = new Renderer("assets/pieces");
        LongPredicate tickSource = ms -> true;
        DoubleFunction<GameSnapshot> snapshotSupplier = zoom -> proxy.latestSnapshot();
        // No wire messages carry MoveEvent/GameOverEvent yet, so this bus never receives a publish -
        // sound/banner effects stay silent over the network until that's added to the protocol.
        EffectsController effects = new EffectsController(new EventBus(), new ClipSoundPlayer("assets"));
        return new GameWindow.GameComponents(tickSource, snapshotSupplier, controller, renderer, effects);
    }

    private static void waitForInitialState(NetworkGameProxy proxy) throws InterruptedException {
        long deadline = System.currentTimeMillis() + INITIAL_STATE_TIMEOUT_MS;
        while (proxy.latestSnapshot() == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        if (proxy.latestSnapshot() == null) {
            throw new IllegalStateException(
                    "Server did not send an initial STATE within " + INITIAL_STATE_TIMEOUT_MS + "ms");
        }
    }
}
