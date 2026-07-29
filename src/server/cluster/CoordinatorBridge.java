package src.server.cluster;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import src.server.coordinator.Coordinator;

import java.nio.charset.StandardCharsets;

public class CoordinatorBridge {

    private final Coordinator coordinator;
    private final Connection nats;

    public CoordinatorBridge(Coordinator coordinator, Connection nats) {
        this.coordinator = coordinator;
        this.nats = nats;
    }

    public void start() {
        Dispatcher dispatcher = nats.createDispatcher(this::onMessage);
        dispatcher.subscribe(ClusterProtocol.LOBBY_INBOUND_SUBJECT);
        dispatcher.subscribe(ClusterProtocol.LOBBY_DISCONNECT_SUBJECT);
        dispatcher.subscribe(ClusterProtocol.MATCH_ENDED_SUBJECT);
    }

    private void onMessage(io.nats.client.Message message) {
        String subject = message.getSubject();
        if (subject.equals(ClusterProtocol.LOBBY_INBOUND_SUBJECT)) {
            onInbound(message.getData());
        } else if (subject.equals(ClusterProtocol.LOBBY_DISCONNECT_SUBJECT)) {
            coordinator.disconnect(new String(message.getData(), StandardCharsets.UTF_8));
        } else if (subject.equals(ClusterProtocol.MATCH_ENDED_SUBJECT)) {
            ClusterProtocol.MatchEndedEvent event = ClusterProtocol.decodeMatchEnded(message.getData());
            coordinator.onMatchEnded(event.matchId(), event.usernames());
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
        coordinator.receive(envelope.connectionId(), envelope.message());
    }
}
