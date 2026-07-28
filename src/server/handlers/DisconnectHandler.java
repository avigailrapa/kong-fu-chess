package src.server.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import src.net.Protocol;
import src.net.messages.DisconnectCountdown;
import src.server.core.Match;
import src.server.core.Session;
import src.server.core.SessionRegistry;
import src.server.matchmaking.ReconnectManager;

public class DisconnectHandler {

    private static final Logger log = LoggerFactory.getLogger(DisconnectHandler.class);

    private final SessionRegistry sessionRegistry;
    private final MatchOrchestrator matchOrchestrator;
    private final ReconnectManager reconnectManager;
    private final MatchBroadcaster broadcaster;

    public DisconnectHandler(SessionRegistry sessionRegistry, MatchOrchestrator matchOrchestrator,
                              ReconnectManager reconnectManager, MatchBroadcaster broadcaster) {
        this.sessionRegistry = sessionRegistry;
        this.matchOrchestrator = matchOrchestrator;
        this.reconnectManager = reconnectManager;
        this.broadcaster = broadcaster;
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
        log.info("{} disconnected - starting resign countdown", disconnected.username());
        reconnectManager.startCountdown(match, disconnected,
                secondsRemaining -> broadcaster.sendQuietly(opponent, Protocol.encode(new DisconnectCountdown(secondsRemaining))),
                () -> {
                    log.warn("{} did not reconnect - auto-resigning", disconnected.username());
                    match.submit(() -> match.engine().resign(disconnected.assignedColor()));
                });
    }
}
