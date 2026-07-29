package src.server.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import src.net.Protocol;
import src.net.messages.LoginCommand;
import src.net.messages.MoveRejected;
import src.net.messages.OpponentReconnected;
import src.net.messages.Welcome;
import src.net.messages.WelcomeBack;
import src.server.auth.UserRecord;
import src.server.auth.UserStore;
import src.server.core.ClientConnection;
import src.server.core.Match;
import src.server.core.Session;
import src.server.core.SessionRegistry;
import src.server.matchmaking.ReconnectManager;

import java.util.Optional;
import java.util.function.Function;

public class SessionAuthHandler {

    private static final Logger log = LoggerFactory.getLogger(SessionAuthHandler.class);

    private final UserStore userStore;
    private final ReconnectManager<Match> reconnectManager;
    private final SessionRegistry sessionRegistry;
    private final MatchBroadcaster broadcaster;
    private final Function<Object, ClientConnection> connectionResolver;

    public SessionAuthHandler(UserStore userStore, ReconnectManager<Match> reconnectManager,
                               SessionRegistry sessionRegistry,
                               MatchBroadcaster broadcaster,
                               Function<Object, ClientConnection> connectionResolver) {
        this.userStore = userStore;
        this.reconnectManager = reconnectManager;
        this.sessionRegistry = sessionRegistry;
        this.broadcaster = broadcaster;
        this.connectionResolver = connectionResolver;
    }

    public String handleLogin(Object conn, LoginCommand l) {
        Optional<ReconnectManager.Pending<Match>> pending = reconnectManager.pendingFor(l.username());
        if (pending.isPresent()) {
            if (!userStore.checkPassword(l.username(), l.password())) {
                log.warn("{} login rejected: bad_credentials", l.username());
                return Protocol.encode(new MoveRejected("bad_credentials"));
            }
            reconnectManager.cancelCountdown(l.username());
            return reconnectSession(conn, pending.get());
        }

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
        Session session = new Session(connectionResolver.apply(conn), l.username(), user.rating());
        sessionRegistry.register(conn, session);
        log.info("{} logged in (rating {})", l.username(), user.rating());
        return Protocol.encode(new Welcome(user.rating()));
    }

    public void reconnectFromCluster(Object connectionId, String username) {
        Optional<ReconnectManager.Pending<Match>> pending = reconnectManager.pendingFor(username);
        if (pending.isEmpty()) {
            log.warn("{} reconnect requested but no pending countdown found", username);
            return;
        }
        reconnectManager.cancelCountdown(username);
        String reply = reconnectSession(connectionId, pending.get());
        connectionResolver.apply(connectionId).send(reply);
    }

    private String reconnectSession(Object conn, ReconnectManager.Pending<Match> pending) {
        Session session = pending.session();
        session.connection(connectionResolver.apply(conn));
        sessionRegistry.register(conn, session);
        log.info("{} reconnected", session.username());
        Session opponent = pending.info().seated().stream().filter(s -> s != session).findFirst().orElse(null);
        if (opponent != null) {
            broadcaster.sendQuietly(opponent, Protocol.encode(new OpponentReconnected()));
        }
        return Protocol.encode(new WelcomeBack(session.rating()));
    }
}
