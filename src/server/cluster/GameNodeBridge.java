package src.server.cluster;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import src.engine.GameOverEvent;
import src.model.Piece;
import src.server.Lobby;
import src.server.core.Match;
import src.server.core.Session;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class GameNodeBridge {

    private final Lobby lobby;
    private final Connection nats;
    private final String nodeId;

    public GameNodeBridge(Lobby lobby, Connection nats, String nodeId) {
        this.lobby = lobby;
        this.nats = nats;
        this.nodeId = nodeId;
    }

    public void start() {
        Dispatcher dispatcher = nats.createDispatcher(this::onMessage);
        dispatcher.subscribe(ClusterProtocol.nodeInboundSubject(nodeId));
        dispatcher.subscribe(ClusterProtocol.nodeDisconnectSubject(nodeId));
        dispatcher.subscribe(ClusterProtocol.createMatchSubject(nodeId));
        dispatcher.subscribe(ClusterProtocol.reconnectSubject(nodeId));
    }

    private void onMessage(io.nats.client.Message message) {
        String subject = message.getSubject();
        if (subject.equals(ClusterProtocol.nodeInboundSubject(nodeId))) {
            onInbound(message.getData());
        } else if (subject.equals(ClusterProtocol.nodeDisconnectSubject(nodeId))) {
            lobby.disconnect(new String(message.getData(), StandardCharsets.UTF_8));
        } else if (subject.equals(ClusterProtocol.createMatchSubject(nodeId))) {
            onCreateMatch(message.getData());
        } else if (subject.equals(ClusterProtocol.reconnectSubject(nodeId))) {
            onReconnect(message.getData());
        } else {
            throw new IllegalStateException("Unhandled cluster subject: " + subject);
        }
    }

    private void onInbound(byte[] payload) {
        ClusterProtocol.Envelope envelope;
        try {
            envelope = ClusterProtocol.decodeInbound(payload);
        } catch (IllegalArgumentException e) {
            return;
        }
        lobby.receive(envelope.connectionId(), envelope.message());
    }

    private void onCreateMatch(byte[] payload) {
        ClusterProtocol.CreateMatchCommand command = ClusterProtocol.decodeCreateMatch(payload);
        List<Lobby.AssignedPlayer> assignments = command.players().stream()
                .map(p -> new Lobby.AssignedPlayer(p.connectionId(), p.username(), p.rating(),
                        Piece.Color.valueOf(p.color())))
                .toList();
        Match match = lobby.createAssignedMatch(command.matchId(), assignments);
        publishMatchEndedOnGameOver(command.matchId(), match);
        match.onNewGame(() -> publishMatchEndedOnGameOver(command.matchId(), match));
    }

    private void publishMatchEndedOnGameOver(String matchId, Match match) {
        match.engine().eventBus().subscribe(GameOverEvent.class, event -> {
            List<String> usernames = match.seated().stream().map(Session::username).toList();
            nats.publish(ClusterProtocol.MATCH_ENDED_SUBJECT,
                    ClusterProtocol.encodeMatchEnded(new ClusterProtocol.MatchEndedEvent(matchId, usernames)));
        });
    }

    private void onReconnect(byte[] payload) {
        ClusterProtocol.ReconnectCommand command = ClusterProtocol.decodeReconnect(payload);
        lobby.reconnectFromCluster(command.connectionId(), command.username());
    }
}
