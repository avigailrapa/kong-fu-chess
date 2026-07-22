package app;

import src.input.BoardMapper;
import src.input.ClickHandler;
import src.model.Position;
import src.net.client.ClientActivityLog;
import src.net.client.LoginResult;
import src.net.client.NetworkGameProxy;
import src.net.client.RoomCreateResult;
import src.net.client.RoomJoinResult;
import src.net.messages.DisconnectCountdown;
import src.net.messages.MatchFound;
import src.net.messages.MatchTimeout;
import src.net.messages.OpponentReconnected;
import src.net.messages.RatingChanged;
import src.view.GameSnapshot;
import src.view.GameWindow;
import src.view.HomeScreen;
import src.view.LoginScreen;
import src.view.Renderer;
import src.view.sound.ClipSoundPlayer;
import src.view.sound.EffectsController;

import javax.swing.*;
import java.io.File;
import java.net.URI;
import java.util.Objects;
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
    private static final String DATA_DIR = "client-data";

    public static void main(String[] args) throws Exception {
        String serverUrl = args.length > 0 ? args[0] : DEFAULT_SERVER_URL;
        new File(DATA_DIR).mkdirs();
        ClientActivityLog activityLog = new ClientActivityLog(DATA_DIR + "/activity.log");

        NetworkGameProxy proxy = new NetworkGameProxy(URI.create(serverUrl), REQUEST_TIMEOUT_MS, activityLog);
        try {
            if (!proxy.connectBlocking(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                System.err.println("Could not connect to server at " + serverUrl);
                proxy.close();
                System.exit(1);
            }
            activityLog.log("connected to " + serverUrl);
        } catch (Exception e) {
            System.err.println("Failed to connect to server at " + serverUrl + ": " + e.getMessage());
            proxy.close();
            System.exit(1);
        }

        System.setProperty("sun.java2d.uiScale", "1");
        System.setProperty("sun.java2d.dpiaware", "true");

        SwingUtilities.invokeLater(() -> openLoginScreen(proxy, activityLog));
    }

    private static void openLoginScreen(NetworkGameProxy proxy, ClientActivityLog activityLog) {
        AtomicReference<LoginScreen> loginScreenRef = new AtomicReference<>();
        LoginScreen loginScreen = new LoginScreen((username, password) -> {
            loginScreenRef.get().setBusy(true);
            LoginResult result = proxy.login(username, password);
            if (!result.accepted()) {
                activityLog.log("login rejected: " + result.reason());
                loginScreenRef.get().showError(describeLoginRejection(result.reason()));
                return;
            }
            activityLog.log(username + " logged in (rating " + result.rating() + ")");
            System.out.println("Welcome, " + username + " (rating " + result.rating() + ")");
            loginScreenRef.get().close();
            if ("reconnected".equals(result.reason())) {
                activityLog.log(username + " reconnected to an active match");
                System.out.println("Reconnected to your active match");
                enterGameWindow(proxy);
            } else {
                openHomeScreen(proxy, activityLog);
            }
        });
        loginScreenRef.set(loginScreen);
        loginScreen.open();
    }

    private static String describeLoginRejection(String reason) {
        return switch (reason) {
            case "bad_credentials" -> "Incorrect password for that username";
            case "timeout" -> "The server did not respond in time";
            default -> "Login failed: " + reason;
        };
    }

    private static void openHomeScreen(NetworkGameProxy proxy, ClientActivityLog activityLog) {
        AtomicReference<HomeScreen> homeScreenRef = new AtomicReference<>();
        HomeScreen homeScreen = new HomeScreen(
                () -> {
                    activityLog.log("play clicked");
                    proxy.play();
                    homeScreenRef.get().showSearching();
                },
                () -> {
                    proxy.cancelPlay();
                    homeScreenRef.get().hideSearching();
                },
                () -> handleRoomCreate(proxy, homeScreenRef.get(), activityLog),
                roomId -> handleRoomJoin(proxy, homeScreenRef.get(), activityLog, roomId));
        homeScreenRef.set(homeScreen);

        proxy.eventBus().subscribe(MatchFound.class, matchFound -> SwingUtilities.invokeLater(() -> {
            homeScreen.closeRoomDialog();
            homeScreen.close();
            startMatch(proxy, matchFound, activityLog);
        }));
        proxy.eventBus().subscribe(MatchTimeout.class,
                matchTimeout -> SwingUtilities.invokeLater(homeScreen::showCantFindMatch));

        homeScreen.open();
    }

    private static void handleRoomCreate(NetworkGameProxy proxy, HomeScreen homeScreen,
                                          ClientActivityLog activityLog) {
        RoomCreateResult result = proxy.createRoom();
        if (!result.accepted()) {
            JOptionPane.showMessageDialog(null, "Could not create room: " + result.reason());
            return;
        }
        activityLog.log("created room " + result.roomId());
        homeScreen.showRoomId(result.roomId());
    }

    private static void handleRoomJoin(NetworkGameProxy proxy, HomeScreen homeScreen,
                                        ClientActivityLog activityLog, String roomId) {
        if (roomId.isBlank()) {
            return;
        }
        RoomJoinResult result = proxy.joinRoom(roomId);
        if (!result.accepted()) {
            JOptionPane.showMessageDialog(null, "Could not join room: " + result.reason());
            return;
        }
        activityLog.log((result.spectating() ? "spectating" : "joined") + " room " + roomId);
        homeScreen.closeRoomDialog();
        if (result.spectating()) {
            homeScreen.close();
            startSpectating(proxy, activityLog);
        }
    }

    private static void startMatch(NetworkGameProxy proxy, MatchFound matchFound, ClientActivityLog activityLog) {
        proxy.resetSnapshot();
        activityLog.log("match found vs " + matchFound.opponentUsername());
        System.out.println("Match found vs " + matchFound.opponentUsername() + " (rating "
                + matchFound.opponentRating() + "), playing as " + matchFound.assignedColor());
        enterGameWindow(proxy);
    }

    private static void enterGameWindow(NetworkGameProxy proxy) {
        Supplier<GameWindow.GameComponents> gameFactory = () -> createGame(proxy);
        GameWindow window = new GameWindow(gameFactory);
        proxy.eventBus().subscribe(DisconnectCountdown.class, disconnectCountdown -> SwingUtilities.invokeLater(
                () -> window.setStatusMessage("Opponent disconnected - resigning in "
                        + disconnectCountdown.secondsRemaining() + "s")));
        proxy.eventBus().subscribe(OpponentReconnected.class, reconnected -> SwingUtilities.invokeLater(
                () -> window.setStatusMessage("♟ Kung Fu Chess ♟")));

        try {
            waitForInitialState(proxy);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        window.open();
    }

    private static void startSpectating(NetworkGameProxy proxy, ClientActivityLog activityLog) {
        proxy.resetSnapshot();
        activityLog.log("spectating a match");
        System.out.println("Spectating a match");

        Supplier<GameWindow.GameComponents> gameFactory = () -> createGame(proxy);
        GameWindow window = new GameWindow(gameFactory);

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
        ClickHandler clickHandler = new ClickHandler(new BoardMapper(BOARD_WIDTH, BOARD_HEIGHT), proxy);
        Renderer renderer = new Renderer("assets/pieces");
        AtomicReference<Position> lastSentSelection = new AtomicReference<>();
        LongPredicate tickSource = ms -> {
            Position selection = clickHandler.getSelectedCell().orElse(null);
            if (!Objects.equals(selection, lastSentSelection.get())) {
                lastSentSelection.set(selection);
                proxy.updateSelection(selection);
            }
            return true;
        };
        DoubleFunction<GameSnapshot> snapshotSupplier = zoom -> proxy.latestSnapshot().withZoom(zoom);
        proxy.eventBus().subscribe(RatingChanged.class, r -> System.out.println("New rating: " + r.newRating()));
        EffectsController effects = new EffectsController(proxy.eventBus(), new ClipSoundPlayer("assets"));
        effects.announceGameStart();
        return new GameWindow.GameComponents(tickSource, snapshotSupplier, clickHandler, renderer, effects);
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
