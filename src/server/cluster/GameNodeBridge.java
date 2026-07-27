package src.server.cluster;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import src.server.Lobby;

import java.nio.charset.StandardCharsets;

public class GameNodeBridge {

    private final Lobby lobby;
    private final Connection nats;

    public GameNodeBridge(Lobby lobby, Connection nats) {
        this.lobby = lobby;
        this.nats = nats;
    }

    public void start() {
        Dispatcher dispatcher = nats.createDispatcher(this::onMessage);
        dispatcher.subscribe(ClusterProtocol.INBOUND_SUBJECT);
        dispatcher.subscribe(ClusterProtocol.DISCONNECT_SUBJECT);
    }

    private void onMessage(io.nats.client.Message message) {
        String subject = message.getSubject();
        if (subject.equals(ClusterProtocol.INBOUND_SUBJECT)) {
            ClusterProtocol.Envelope envelope = ClusterProtocol.decodeInbound(message.getData());
            lobby.receive(envelope.connectionId(), envelope.message());
        } else if (subject.equals(ClusterProtocol.DISCONNECT_SUBJECT)) {
            lobby.disconnect(new String(message.getData(), StandardCharsets.UTF_8));
        } else {
            throw new IllegalStateException("Unhandled cluster subject: " + subject);
        }
    }
}
