package src.server.cluster;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import src.server.matchmaker.Matchmaker;

import java.nio.charset.StandardCharsets;

public class MatchmakerBridge {

    private final Matchmaker matchmaker;
    private final Connection nats;

    public MatchmakerBridge(Matchmaker matchmaker, Connection nats) {
        this.matchmaker = matchmaker;
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
            matchmaker.disconnect(new String(message.getData(), StandardCharsets.UTF_8));
        } else if (subject.equals(ClusterProtocol.MATCH_ENDED_SUBJECT)) {
            ClusterProtocol.MatchEndedEvent event = ClusterProtocol.decodeMatchEnded(message.getData());
            matchmaker.onMatchEnded(event.matchId(), event.usernames());
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
        matchmaker.receive(envelope.connectionId(), envelope.message());
    }
}
