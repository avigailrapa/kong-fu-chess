package src.server.handlers;

import src.net.Protocol;
import src.net.messages.LoginCommand;
import src.net.messages.MoveRejected;
import src.net.messages.OpponentReconnected;
import src.net.messages.Welcome;
import src.net.messages.WelcomeBack;
import src.server.auth.UserRecord;
import src.server.auth.UserStore;
import src.server.core.ActivityLog;
import src.server.core.ClientConnection;
import src.server.core.Session;
import src.server.core.SessionRegistry;
import src.server.matchmaking.ReconnectManager;

import java.util.Optional;
import java.util.function.Function;

public class SessionAuthHandler {

    private final UserStore userStore;
    private final ReconnectManager reconnectManager;
    private final SessionRegistry sessionRegistry;
    private final MatchBroadcaster broadcaster;
    private final ActivityLog activityLog;
    private final Function<Object, ClientConnection> connectionResolver;

    public SessionAuthHandler(UserStore userStore, ReconnectManager reconnectManager, SessionRegistry sessionRegistry,
                               MatchBroadcaster broadcaster, ActivityLog activityLog,
                               Function<Object, ClientConnection> connectionResolver) {
        this.userStore = userStore;
        this.reconnectManager = reconnectManager;
        this.sessionRegistry = sessionRegistry;
        this.broadcaster = broadcaster;
        this.activityLog = activityLog;
        this.connectionResolver = connectionResolver;
    }

    public String handleLogin(Object conn, LoginCommand l) {
        Optional<ReconnectManager.Pending> pending = reconnectManager.pendingFor(l.username());
        if (pending.isPresent()) {
            if (!userStore.checkPassword(l.username(), l.password())) {
                activityLog.log(l.username() + " login rejected: bad_credentials");
                return Protocol.encode(new MoveRejected("bad_credentials"));
            }
            reconnectManager.cancelCountdown(l.username());
            return reconnectSession(conn, pending.get());
        }

        Optional<UserRecord> existing = userStore.find(l.username());
        UserRecord user;
        if (existing.isPresent()) {
            if (!userStore.checkPassword(l.username(), l.password())) {
                activityLog.log(l.username() + " login rejected: bad_credentials");
                return Protocol.encode(new MoveRejected("bad_credentials"));
            }
            user = existing.get();
        } else {
            user = userStore.createUser(l.username(), l.password());
        }
        Session session = new Session(connectionResolver.apply(conn), l.username(), user.rating());
        sessionRegistry.register(conn, session);
        activityLog.log(l.username() + " logged in (rating " + user.rating() + ")");
        return Protocol.encode(new Welcome(user.rating()));
    }

    private String reconnectSession(Object conn, ReconnectManager.Pending pending) {
        Session session = pending.session();
        session.connection(connectionResolver.apply(conn));
        sessionRegistry.register(conn, session);
        activityLog.log(session.username() + " reconnected");
        Session opponent = pending.match().seated().stream().filter(s -> s != session).findFirst().orElse(null);
        if (opponent != null) {
            broadcaster.sendQuietly(opponent, Protocol.encode(new OpponentReconnected()));
        }
        return Protocol.encode(new WelcomeBack(session.rating()));
    }
}
