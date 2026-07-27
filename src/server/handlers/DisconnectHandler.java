package src.server.handlers;

import src.net.Protocol;
import src.net.messages.DisconnectCountdown;
import src.server.core.ActivityLog;
import src.server.core.Match;
import src.server.core.Session;
import src.server.core.SessionRegistry;
import src.server.matchmaking.ReconnectManager;

public class DisconnectHandler {

    private final SessionRegistry sessionRegistry;
    private final MatchOrchestrator matchOrchestrator;
    private final ReconnectManager reconnectManager;
    private final MatchBroadcaster broadcaster;
    private final ActivityLog activityLog;

    public DisconnectHandler(SessionRegistry sessionRegistry, MatchOrchestrator matchOrchestrator,
                              ReconnectManager reconnectManager, MatchBroadcaster broadcaster, ActivityLog activityLog) {
        this.sessionRegistry = sessionRegistry;
        this.matchOrchestrator = matchOrchestrator;
        this.reconnectManager = reconnectManager;
        this.broadcaster = broadcaster;
        this.activityLog = activityLog;
    }

    public void disconnect(Object conn) {
        Session session = sessionRegistry.unregister(conn);
        if (session == null) {
            return;
        }
        matchOrchestrator.cancelMatchmaking(session);
        Match match = sessionRegistry.matchFor(session);
        if (match != null && match.seated().contains(session)) {
            startDisconnectCountdown(match, session);
        }
    }

    private void startDisconnectCountdown(Match match, Session disconnected) {
        Session opponent = match.seated().stream().filter(s -> s != disconnected).findFirst().orElse(null);
        if (opponent == null) {
            return;
        }
        activityLog.log(disconnected.username() + " disconnected - starting resign countdown");
        reconnectManager.startCountdown(match, disconnected,
                secondsRemaining -> broadcaster.sendQuietly(opponent, Protocol.encode(new DisconnectCountdown(secondsRemaining))),
                () -> {
                    activityLog.log(disconnected.username() + " did not reconnect - auto-resigning");
                    match.submit(() -> match.engine().resign(disconnected.assignedColor()));
                });
    }
}
