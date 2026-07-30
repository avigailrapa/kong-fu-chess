package app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import src.input.BoardMapper;
import src.input.ClickHandler;
import src.model.Position;
import src.net.client.ApiGatewayClient;
import src.net.client.LoginResult;
import src.net.client.NetworkGameProxy;
import src.net.client.RoomCreateResult;
import src.net.client.RoomJoinResult;
import src.net.client.TokenResult;
import src.net.messages.DisconnectCountdown;
import src.net.messages.MatchFound;
import src.net.messages.MatchTimeout;
import src.net.messages.OpponentReconnected;
import src.net.messages.RatingChanged;
import src.view.snapshot.GameSnapshot;
import src.view.screens.GameWindow;
import src.view.screens.HomeScreen;
import src.view.screens.LoginScreen;
import src.view.Renderer;
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

    private static final String DEFAULT_SERVER_URL = "ws://localhost:" + WsGatewayMain.DEFAULT_PORT;
    private static final String DEFAULT_API_URL = "http://localhost:" + ApiGatewayMain.DEFAULT_PORT;
    private static final long REQUEST_TIMEOUT_MS = 2000;
    private static final long CONNECT_TIMEOUT_SECONDS = 5;
    private static final long INITIAL_STATE_TIMEOUT_MS = 5000;
    private static final long NEW_GAME_CONFIRM_TIMEOUT_MS = 5000;
    private static final long POLL_INTERVAL_MS = 20;
    private static final int BOARD_WIDTH = 8;
    private static final int BOARD_HEIGHT = 8;
    private static final String DATA_DIR = "data/client";

    public static void main(String[] args) throws Exception {
        new File(DATA_DIR).mkdirs();
        System.setProperty("LOG_FILE", DATA_DIR + "/client.log");
        Logger log = LoggerFactory.getLogger(ClientMain.class);

        String serverUrl = args.length > 0 ? args[0] : DEFAULT_SERVER_URL;
        String apiUrl = args.length > 1 ? args[1] : DEFAULT_API_URL;
        NetworkGameProxy proxy = new NetworkGameProxy(URI.create(serverUrl), REQUEST_TIMEOUT_MS);
        try {
            if (!proxy.connectBlocking(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.error("Could not connect to server at {}", serverUrl);
                proxy.close();
                System.exit(1);
            }
            log.info("connected to {}", serverUrl);
        } catch (Exception e) {
            log.error("Failed to connect to server at {}", serverUrl, e);
            proxy.close();
            System.exit(1);
        }

        AppSupport.disableHiDpiScaling();

        ApiGatewayClient apiClient = new ApiGatewayClient(apiUrl);
        SwingUtilities.invokeLater(() -> openLoginScreen(proxy, apiClient));
    }

    private static void openLoginScreen(NetworkGameProxy proxy, ApiGatewayClient apiClient) {
        Logger log = LoggerFactory.getLogger(ClientMain.class);
        AtomicReference<LoginScreen> loginScreenRef = new AtomicReference<>();
        LoginScreen loginScreen = new LoginScreen((username, password) -> {
            loginScreenRef.get().setBusy(true);
            LoginResult result = authenticate(proxy, apiClient, username, password);
            if (!result.accepted()) {
                log.warn("login rejected: {}", result.reason());
                loginScreenRef.get().showError(describeLoginRejection(result.reason()));
                return;
            }
            log.info("{} logged in (rating {})", username, result.rating());
            loginScreenRef.get().close();
            if ("reconnected".equals(result.reason())) {
                log.info("{} reconnected to an active match", username);
                enterGameWindow(proxy);
            } else {
                openHomeScreen(proxy);
            }
        });
        loginScreenRef.set(loginScreen);
        loginScreen.open();
    }

    private static LoginResult authenticate(NetworkGameProxy proxy, ApiGatewayClient apiClient, String username,
            String password) {
        TokenResult tokenResult = apiClient.login(username, password);
        if (tokenResult.accepted()) {
            return proxy.auth(tokenResult.token());
        }
        if ("unreachable".equals(tokenResult.reason())) {
            return proxy.login(username, password);
        }
        return new LoginResult(false, 0, tokenResult.reason());
    }

    private static String describeLoginRejection(String reason) {
        return switch (reason) {
            case "bad_credentials" -> "Incorrect password for that username";
            case "timeout" -> "The server did not respond in time";
            default -> "Login failed: " + reason;
        };
    }

    private static void openHomeScreen(NetworkGameProxy proxy) {
        Logger log = LoggerFactory.getLogger(ClientMain.class);
        AtomicReference<HomeScreen> homeScreenRef = new AtomicReference<>();
        HomeScreen homeScreen = new HomeScreen(
                () -> {
                    log.debug("play clicked");
                    proxy.play();
                    homeScreenRef.get().showSearching();
                },
                () -> {
                    proxy.cancelPlay();
                    homeScreenRef.get().hideSearching();
                },
                () -> handleRoomCreate(proxy, homeScreenRef.get()),
                roomId -> handleRoomJoin(proxy, homeScreenRef.get(), roomId));
        homeScreenRef.set(homeScreen);

        proxy.eventBus().subscribe(MatchFound.class, matchFound -> SwingUtilities.invokeLater(() -> {
            homeScreen.closeRoomDialog();
            homeScreen.close();
            startMatch(proxy, matchFound);
        }));
        proxy.eventBus().subscribe(MatchTimeout.class,
                matchTimeout -> SwingUtilities.invokeLater(homeScreen::showCantFindMatch));

        homeScreen.open();
    }

    private static void handleRoomCreate(NetworkGameProxy proxy, HomeScreen homeScreen) {
        Logger log = LoggerFactory.getLogger(ClientMain.class);
        RoomCreateResult result = proxy.createRoom();
        if (!result.accepted()) {
            JOptionPane.showMessageDialog(null, "Could not create room: " + result.reason());
            return;
        }
        log.info("created room {}", result.roomId());
        homeScreen.showRoomId(result.roomId());
    }

    private static void handleRoomJoin(NetworkGameProxy proxy, HomeScreen homeScreen, String roomId) {
        Logger log = LoggerFactory.getLogger(ClientMain.class);
        if (roomId.isBlank()) {
            return;
        }
        RoomJoinResult result = proxy.joinRoom(roomId);
        if (!result.accepted()) {
            JOptionPane.showMessageDialog(null, "Could not join room: " + result.reason());
            return;
        }
        log.info("{} room {}", result.spectating() ? "spectating" : "joined", roomId);
        homeScreen.closeRoomDialog();
        if (result.spectating()) {
            homeScreen.close();
            startSpectating(proxy);
        }
    }

    private static void startMatch(NetworkGameProxy proxy, MatchFound matchFound) {
        Logger log = LoggerFactory.getLogger(ClientMain.class);
        proxy.resetSnapshot();
        log.info("match found vs {} (rating {}), playing as {}", matchFound.opponentUsername(),
                matchFound.opponentRating(), matchFound.assignedColor());
        enterGameWindow(proxy);
    }

    private static void enterGameWindow(NetworkGameProxy proxy) {
        Supplier<GameWindow.GameComponents> gameFactory = () -> createGame(proxy);
        GameWindow window = new GameWindow(gameFactory);
        proxy.eventBus().subscribe(DisconnectCountdown.class, disconnectCountdown -> SwingUtilities.invokeLater(
                () -> window.setStatusMessage("Opponent disconnected - resigning in "
                        + disconnectCountdown.secondsRemaining() + "s")));
        proxy.eventBus().subscribe(OpponentReconnected.class, reconnected -> SwingUtilities.invokeLater(
                window::clearStatusMessage));
        openWindow(proxy, window);
    }

    private static void startSpectating(NetworkGameProxy proxy) {
        Logger log = LoggerFactory.getLogger(ClientMain.class);
        proxy.resetSnapshot();
        log.info("spectating a match");

        Supplier<GameWindow.GameComponents> gameFactory = () -> createGame(proxy);
        GameWindow window = new GameWindow(gameFactory);
        openWindow(proxy, window);
    }

    private static void openWindow(NetworkGameProxy proxy, GameWindow window) {
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
        Renderer renderer = new Renderer(Renderer.DEFAULT_PIECES_ROOT);
        AtomicReference<Position> lastSentSelection = new AtomicReference<>();
        LongPredicate tickSource = ms -> {
            Position selection = clickHandler.selectedCell().orElse(null);
            if (!Objects.equals(selection, lastSentSelection.get())) {
                lastSentSelection.set(selection);
                proxy.updateSelection(selection);
            }
            return true;
        };
        DoubleFunction<GameSnapshot> snapshotSupplier = zoom -> proxy.latestSnapshot().withZoom(zoom);
        Logger log = LoggerFactory.getLogger(ClientMain.class);
        proxy.eventBus().subscribe(RatingChanged.class, r -> log.info("New rating: {}", r.newRating()));
        EffectsController effects = AppSupport.startEffects(proxy.eventBus());
        return new GameWindow.GameComponents(tickSource, snapshotSupplier, clickHandler, renderer, effects);
    }

    private static void awaitNewGameConfirmed(NetworkGameProxy proxy) {
        long deadline = System.currentTimeMillis() + NEW_GAME_CONFIRM_TIMEOUT_MS;
        while (proxy.latestSnapshot().gameOver() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void waitForInitialState(NetworkGameProxy proxy) throws InterruptedException {
        long deadline = System.currentTimeMillis() + INITIAL_STATE_TIMEOUT_MS;
        while (proxy.latestSnapshot() == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_INTERVAL_MS);
        }
        if (proxy.latestSnapshot() == null) {
            throw new IllegalStateException(
                    "Server did not send an initial STATE within " + INITIAL_STATE_TIMEOUT_MS + "ms");
        }
    }
}
