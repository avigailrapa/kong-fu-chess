package src.server.cluster;

import io.nats.client.Connection;
import src.server.ClientConnection;

import java.nio.charset.StandardCharsets;

public class NatsClientConnection implements ClientConnection {

    private final Connection nats;
    private final String connectionId;

    public NatsClientConnection(Connection nats, String connectionId) {
        this.nats = nats;
        this.connectionId = connectionId;
    }

    @Override
    public void send(String text) {
        nats.publish(ClusterProtocol.outboundSubject(connectionId), text.getBytes(StandardCharsets.UTF_8));
    }
}
