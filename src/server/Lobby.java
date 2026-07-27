package src.server;

import src.net.MalformedMessageException;
import src.net.Protocol;
import src.net.messages.CancelPlayCommand;
import src.net.messages.JumpCommand;
import src.net.messages.LoginCommand;
import src.net.messages.MoveCommand;
import src.net.messages.MoveRejected;
import src.net.messages.NewGameCommand;
import src.net.messages.PlayCommand;
import src.net.messages.RoomCreateCommand;
import src.net.messages.RoomJoinCommand;
import src.net.messages.SelectCommand;
import src.net.messages.WireMessage;
import src.server.auth.UserStore;
import src.server.core.ActivityLog;
import src.server.core.ClientConnection;
import src.server.core.Match;
import src.server.core.Session;
import src.server.core.SessionRegistry;
import src.server.handlers.DisconnectHandler;
import src.server.handlers.GameActionHandler;
import src.server.handlers.MatchBroadcaster;
import src.server.handlers.MatchOrchestrator;
import src.server.handlers.RatingService;
import src.server.handlers.SessionAuthHandler;
import src.server.matchmaking.ReconnectManager;

import java.util.function.Function;

public class Lobby {

    private final ActivityLog activityLog;
    private final Function<Object, ClientConnection> connectionResolver;
    private final SessionRegistry sessionRegistry;
    private final MatchBroadcaster broadcaster;
    private final SessionAuthHandler authHandler;
    private final MatchOrchestrator matchOrchestrator;
    private final GameActionHandler gameActionHandler;
    private final DisconnectHandler disconnectHandler;

    public Lobby(UserStore userStore, long tickIntervalMs, int disconnectCountdownSeconds, ActivityLog activityLog,
                 Function<Object, ClientConnection> connectionResolver) {
        this.activityLog = activityLog;
        this.connectionResolver = connectionResolver;
        this.sessionRegistry = new SessionRegistry();
        this.broadcaster = new MatchBroadcaster(activityLog);
        RatingService ratingService = new RatingService(userStore, broadcaster);
        ReconnectManager reconnectManager = new ReconnectManager(disconnectCountdownSeconds);
        this.matchOrchestrator = new MatchOrchestrator(tickIntervalMs, activityLog, sessionRegistry, broadcaster,
                ratingService);
        this.authHandler = new SessionAuthHandler(userStore, reconnectManager, sessionRegistry, broadcaster,
                activityLog, connectionResolver);
        this.gameActionHandler = new GameActionHandler(sessionRegistry);
        this.disconnectHandler = new DisconnectHandler(sessionRegistry, matchOrchestrator, reconnectManager,
                broadcaster, activityLog);
    }

    public Match matchFor(Object conn) {
        return sessionRegistry.matchFor(conn);
    }

    private ClientConnection connectionFor(Object conn) {
        Session session = sessionRegistry.sessionFor(conn);
        return session != null ? session.connection() : connectionResolver.apply(conn);
    }

    public void receive(Object conn, String message) {
        activityLog.log("CLIENT_TO_SERVER " + message);
        Match match = matchFor(conn);
        Runnable task = () -> {
            String reply = handleMessage(conn, message);
            connectionFor(conn).send(reply);
            activityLog.log("SERVER_TO_CLIENT " + reply);
            if (match != null) {
                broadcaster.broadcastState(match);
            }
        };
        if (match != null) {
            match.submit(task);
        } else {
            task.run();
        }
    }

    public void disconnect(Object conn) {
        disconnectHandler.disconnect(conn);
    }

    public String handleMessage(Object conn, String message) {
        WireMessage parsed;
        try {
            parsed = Protocol.parse(message);
        } catch (MalformedMessageException e) {
            return Protocol.encode(new MoveRejected("malformed"));
        }
        return switch (parsed) {
            case LoginCommand l -> authHandler.handleLogin(conn, l);
            case PlayCommand p -> matchOrchestrator.handlePlay(conn, p);
            case CancelPlayCommand c -> matchOrchestrator.handleCancelPlay(conn, c);
            case MoveCommand m -> gameActionHandler.handleMove(conn, m);
            case JumpCommand j -> gameActionHandler.handleJump(conn, j);
            case SelectCommand sel -> gameActionHandler.handleSelect(conn, sel);
            case NewGameCommand ng -> matchOrchestrator.handleNewGame(conn, ng);
            case RoomCreateCommand r -> matchOrchestrator.handleRoomCreate(conn, r);
            case RoomJoinCommand r -> matchOrchestrator.handleRoomJoin(conn, r);
            default -> Protocol.encode(new MoveRejected("unexpected_message"));
        };
    }
}
