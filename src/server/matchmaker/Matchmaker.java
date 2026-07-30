package src.server.matchmaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import src.model.Piece;
import src.net.MalformedMessageException;
import src.net.Protocol;
import src.net.messages.AuthCommand;
import src.net.messages.CancelPlayCommand;
import src.net.messages.LoginCommand;
import src.net.messages.MatchTimeout;
import src.net.messages.MoveAccepted;
import src.net.messages.MoveRejected;
import src.net.messages.PlayCommand;
import src.net.messages.RoomCreateCommand;
import src.net.messages.RoomId;
import src.net.messages.RoomJoinCommand;
import src.net.messages.Welcome;
import src.net.messages.WireMessage;
import src.server.allocator.ConnectionDirectory;
import src.server.auth.TokenStore;
import src.server.auth.UserRecord;
import src.server.auth.UserStore;
import src.server.core.ClientConnection;
import src.server.core.Session;
import src.server.core.SessionRegistry;
import src.server.matchmaking.MatchmakingQueue;
import src.server.matchmaking.RoomRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Matchmaker {

    private static final Logger log = LoggerFactory.getLogger(Matchmaker.class);
    private static final long MATCHMAKING_TIMEOUT_MS = 60_000;
    private static final int RATING_WINDOW = 100;

    public record PlayerAssignment(String connectionId, String username, int rating, Piece.Color color) {
    }

    public interface MatchDispatcher {
        void assign(String matchId, List<PlayerAssignment> players);

        void reconnect(String nodeId, String username, String connectionId);
    }

    private static final class RoomState {
        private final List<Session> seated = new ArrayList<>();
    }

    private final UserStore userStore;
    private final TokenStore tokenStore;
    private final ConnectionDirectory directory;
    private final MatchDispatcher dispatcher;
    private final Function<String, ClientConnection> connectionResolver;
    private final SessionRegistry sessionRegistry = new SessionRegistry();
    private final Map<Session, String> connectionIdBySession = new ConcurrentHashMap<>();
    private final MatchmakingQueue matchmakingQueue;
    private final RoomRegistry<RoomState> roomRegistry;

    public Matchmaker(UserStore userStore, TokenStore tokenStore, ConnectionDirectory directory,
                       MatchDispatcher dispatcher, Function<String, ClientConnection> connectionResolver) {
        this.userStore = userStore;
        this.tokenStore = tokenStore;
        this.directory = directory;
        this.dispatcher = dispatcher;
        this.connectionResolver = connectionResolver;
        this.matchmakingQueue = new MatchmakingQueue(this::onPaired, this::onMatchTimeout,
                MATCHMAKING_TIMEOUT_MS, RATING_WINDOW);
        this.roomRegistry = new RoomRegistry<>(RoomState::new,
                (room, session) -> room.seated.add(session),
                this::onRoomReady,
                (room, session) -> log.warn("{} tried to spectate a clustered room - not supported yet",
                        session.username()));
    }

    public void receive(String connectionId, String message) {
        log.debug("CLIENT_TO_SERVER {}", message);
        String reply = handleMessage(connectionId, message);
        if (reply != null) {
            connectionResolver.apply(connectionId).send(reply);
            log.debug("SERVER_TO_CLIENT {}", reply);
        }
    }

    public void disconnect(String connectionId) {
        Session session = sessionRegistry.unregister(connectionId);
        if (session == null) {
            return;
        }
        matchmakingQueue.cancel(session);
        connectionIdBySession.remove(session);
        directory.clearConnection(connectionId);
    }

    public int queuedPlayerCount() {
        return matchmakingQueue.size();
    }

    public void onMatchEnded(String matchId, List<String> usernames) {
        for (String username : usernames) {
            directory.clearUser(username);
        }
        log.debug("match {} ended, cleared routing for {}", matchId, usernames);
    }

    private String handleMessage(String connectionId, String message) {
        WireMessage parsed;
        try {
            parsed = Protocol.parse(message);
        } catch (MalformedMessageException e) {
            log.warn("malformed message: {}", message);
            return Protocol.encode(new MoveRejected("malformed"));
        }
        return switch (parsed) {
            case LoginCommand l -> handleLogin(connectionId, l);
            case AuthCommand a -> handleAuth(connectionId, a);
            case PlayCommand _ -> handlePlay(connectionId);
            case CancelPlayCommand _ -> handleCancelPlay(connectionId);
            case RoomCreateCommand _ -> handleRoomCreate(connectionId);
            case RoomJoinCommand r -> handleRoomJoin(connectionId, r);
            default -> Protocol.encode(new MoveRejected("unexpected_message"));
        };
    }

    private String handleLogin(String connectionId, LoginCommand l) {
        Optional<UserRecord> existing = userStore.find(l.username());
        UserRecord user;
        if (existing.isPresent()) {
            if (!userStore.checkPassword(l.username(), l.password())) {
                log.warn("{} login rejected: bad_credentials", l.username());
                return Protocol.encode(new MoveRejected("bad_credentials"));
            }
            user = existing.get();
        } else {
            user = userStore.createUser(l.username(), l.password());
        }
        return establishSession(connectionId, user.username(), user.rating());
    }

    private String handleAuth(String connectionId, AuthCommand a) {
        Optional<String> username = tokenStore.resolve(a.token());
        if (username.isEmpty()) {
            log.warn("auth rejected: token_mismatch");
            return Protocol.encode(new MoveRejected("token_mismatch"));
        }
        Optional<UserRecord> user = userStore.find(username.get());
        if (user.isEmpty()) {
            log.warn("{} auth rejected: token_mismatch", username.get());
            return Protocol.encode(new MoveRejected("token_mismatch"));
        }
        return establishSession(connectionId, user.get().username(), user.get().rating());
    }

    private String establishSession(String connectionId, String username, int rating) {
        Optional<String> reconnectNodeId = directory.nodeForUser(username);
        if (reconnectNodeId.isPresent()) {
            directory.assignConnection(connectionId, reconnectNodeId.get());
            dispatcher.reconnect(reconnectNodeId.get(), username, connectionId);
            log.info("{} reconnecting via {}", username, reconnectNodeId.get());
            return null;
        }
        registerSession(connectionId, username, rating);
        log.info("{} logged in (rating {})", username, rating);
        return Protocol.encode(new Welcome(rating));
    }

    private void registerSession(String connectionId, String username, int rating) {
        Session session = new Session(connectionResolver.apply(connectionId), username, rating);
        sessionRegistry.register(connectionId, session);
        connectionIdBySession.put(session, connectionId);
    }

    private String handlePlay(String connectionId) {
        return requireSession(connectionId, session -> {
            matchmakingQueue.enqueue(session);
            return Protocol.encode(new MoveAccepted());
        });
    }

    private String handleCancelPlay(String connectionId) {
        Session session = sessionRegistry.sessionFor(connectionId);
        if (session != null) {
            matchmakingQueue.cancel(session);
        }
        return Protocol.encode(new MoveAccepted());
    }

    private String handleRoomCreate(String connectionId) {
        return requireSession(connectionId, session -> {
            String roomId = roomRegistry.createRoom(session);
            log.info("{} created room {}", session.username(), roomId);
            return Protocol.encode(new RoomId(roomId));
        });
    }

    private String handleRoomJoin(String connectionId, RoomJoinCommand r) {
        return requireSession(connectionId, session -> {
            RoomRegistry.JoinOutcome outcome = roomRegistry.joinRoom(r.roomId(), session);
            return switch (outcome) {
                case SEATED_BLACK -> {
                    log.info("{} joined room {} as black", session.username(), r.roomId());
                    yield Protocol.encode(new RoomId(r.roomId()));
                }
                case SPECTATING -> Protocol.encode(new MoveRejected("spectating_not_supported_yet"));
                case NOT_FOUND -> Protocol.encode(new MoveRejected("room_not_found"));
            };
        });
    }

    private String requireSession(String connectionId, Function<Session, String> onPresent) {
        Session session = sessionRegistry.sessionFor(connectionId);
        return session == null ? Protocol.encode(new MoveRejected("not_logged_in")) : onPresent.apply(session);
    }

    private void onPaired(Session a, Session b) {
        dispatchNewMatch(a, b);
    }

    private void onRoomReady(RoomState room) {
        dispatchNewMatch(room.seated.get(0), room.seated.get(1));
    }

    private void onMatchTimeout(Session session) {
        String connectionId = connectionIdBySession.get(session);
        if (connectionId != null) {
            connectionResolver.apply(connectionId).send(Protocol.encode(new MatchTimeout()));
        }
    }

    private void dispatchNewMatch(Session white, Session black) {
        String matchId = UUID.randomUUID().toString();
        List<PlayerAssignment> players = List.of(
                new PlayerAssignment(takeConnectionId(white), white.username(), white.rating(), Piece.Color.WHITE),
                new PlayerAssignment(takeConnectionId(black), black.username(), black.rating(), Piece.Color.BLACK));
        log.info("{} vs {} - paired, requesting allocation", white.username(), black.username());
        dispatcher.assign(matchId, players);
    }

    private String takeConnectionId(Session session) {
        String connectionId = connectionIdBySession.remove(session);
        sessionRegistry.unregister(connectionId);
        return connectionId;
    }
}
