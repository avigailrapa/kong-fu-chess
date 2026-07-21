package app;

import src.input.BoardMapper;
import src.input.Controller;
import src.model.Position;
import src.net.DisconnectCountdown;
import src.net.LoginResult;
import src.net.MatchFound;
import src.net.MatchTimeout;
import src.net.NetworkGameProxy;
import src.net.RatingChanged;
import src.view.GameSnapshot;
import src.view.GameWindow;
import src.view.HomeScreen;
import src.view.Renderer;
import src.view.sound.ClipSoundPlayer;
import src.view.sound.EffectsController;

import javax.swing.*;
import java.io.Console;
import java.net.URI;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleFunction;
import java.util.function.LongPredicate;
import java.util.function.Supplier;

public class ClientMain {

    private static final String DEFAULT_SERVER_URL = "ws://localhost:8887";
    private static final long REQUEST_TIMEOUT_MS = 2000;
    private static final long CONNECT_TIMEOUT_SECONDS = 5;
    private static final long INITIAL_STATE_TIMEOUT_MS = 5000;
    private static final long NEW_GAME_CONFIRM_TIMEOUT_MS = 5000;
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
            login(proxy);
        } catch (Exception e) {
            System.err.println("Failed to connect to server at " + serverUrl + ": " + e.getMessage());
            proxy.close();
            System.exit(1);
        }

        System.setProperty("sun.java2d.uiScale", "1");
        System.setProperty("sun.java2d.dpiaware", "true");

        SwingUtilities.invokeLater(() -> openHomeScreen(proxy));
    }

    private static void login(NetworkGameProxy proxy) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Username: ");
        String username = scanner.nextLine();
        String password = readPassword(scanner);

        LoginResult result = proxy.login(username, password);
        if (!result.accepted()) {
            System.err.println("Login rejected: " + result.reason());
            proxy.close();
            System.exit(1);
        }
        System.out.println("Welcome, " + username + " (rating " + result.rating() + ")");
    }

    private static String readPassword(Scanner scanner) {
        Console console = System.console();
        if (console != null) {
            return new String(console.readPassword("Password: "));
        }
        System.out.print("Password: ");
        return scanner.nextLine();
    }

    private static void openHomeScreen(NetworkGameProxy proxy) {
        AtomicReference<HomeScreen> homeScreenRef = new AtomicReference<>();
        HomeScreen homeScreen = new HomeScreen(
                () -> {
                    proxy.play();
                    homeScreenRef.get().showSearching();
                },
                () -> {
                    proxy.cancelPlay();
                    homeScreenRef.get().hideSearching();
                });
        homeScreenRef.set(homeScreen);

        proxy.eventBus().subscribe(MatchFound.class, matchFound -> SwingUtilities.invokeLater(() -> {
            homeScreen.close();
            startMatch(proxy, matchFound);
        }));
        proxy.eventBus().subscribe(MatchTimeout.class,
                matchTimeout -> SwingUtilities.invokeLater(homeScreen::showCantFindMatch));

        homeScreen.open();
    }

    private static void startMatch(NetworkGameProxy proxy, MatchFound matchFound) {
        proxy.resetSnapshot();
        System.out.println("Match found vs " + matchFound.opponentUsername() + " (rating "
                + matchFound.opponentRating() + "), playing as " + matchFound.assignedColor());

        Supplier<GameWindow.GameComponents> gameFactory = () -> createGame(proxy);
        GameWindow window = new GameWindow(gameFactory);
        proxy.eventBus().subscribe(DisconnectCountdown.class, disconnectCountdown -> SwingUtilities.invokeLater(
                () -> window.setStatusMessage("Opponent disconnected - resigning in "
                        + disconnectCountdown.secondsRemaining() + "s")));

        try {
            waitForInitialState(proxy);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        window.open();
    }

    private static GameWindow.GameComponents createGame(NetworkGameProxy proxy) {
        GameSnapshot current = proxy.latestSnapshot();
        if (current != null && current.gameOver()) {
            proxy.newGame();
            awaitNewGameConfirmed(proxy);
        }
        Controller controller = new Controller(new BoardMapper(BOARD_WIDTH, BOARD_HEIGHT), proxy);
        Renderer renderer = new Renderer("assets/pieces");
        AtomicReference<Position> lastSentSelection = new AtomicReference<>();
        LongPredicate tickSource = ms -> {
            Position selection = controller.getSelectedCell().orElse(null);
            if (!Objects.equals(selection, lastSentSelection.get())) {
                lastSentSelection.set(selection);
                proxy.updateSelection(selection);
            }
            return true;
        };
        DoubleFunction<GameSnapshot> snapshotSupplier = zoom -> proxy.latestSnapshot();
        proxy.eventBus().subscribe(RatingChanged.class, r -> System.out.println("New rating: " + r.newRating()));
        EffectsController effects = new EffectsController(proxy.eventBus(), new ClipSoundPlayer("assets"));
        effects.announceGameStart();
        return new GameWindow.GameComponents(tickSource, snapshotSupplier, controller, renderer, effects);
    }

    private static void awaitNewGameConfirmed(NetworkGameProxy proxy) {
        long deadline = System.currentTimeMillis() + NEW_GAME_CONFIRM_TIMEOUT_MS;
        while (proxy.latestSnapshot().gameOver() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
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
