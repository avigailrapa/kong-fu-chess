package src.server.cluster;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WsGateway extends WebSocketServer {

    private final Connection nats;
    private final Map<WebSocket, String> connectionIdsByConn = new ConcurrentHashMap<>();
    private final Map<String, WebSocket> connsByConnectionId = new ConcurrentHashMap<>();

    public WsGateway(InetSocketAddress address, Connection nats) {
        super(address);
        this.nats = nats;
        Dispatcher dispatcher = nats.createDispatcher(this::onOutbound);
        dispatcher.subscribe(ClusterProtocol.OUTBOUND_WILDCARD_SUBJECT);
    }

    private void onOutbound(io.nats.client.Message message) {
        String connectionId = ClusterProtocol.connectionIdFromOutboundSubject(message.getSubject());
        WebSocket conn = connsByConnectionId.get(connectionId);
        if (conn == null || !conn.isOpen()) {
            return;
        }
        try {
            conn.send(new String(message.getData(), StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
        }
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String connectionId = UUID.randomUUID().toString();
        connectionIdsByConn.put(conn, connectionId);
        connsByConnectionId.put(connectionId, conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String connectionId = connectionIdsByConn.remove(conn);
        if (connectionId != null) {
            connsByConnectionId.remove(connectionId);
            nats.publish(ClusterProtocol.DISCONNECT_SUBJECT, connectionId.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String connectionId = connectionIdsByConn.get(conn);
        if (connectionId != null) {
            nats.publish(ClusterProtocol.INBOUND_SUBJECT, ClusterProtocol.encodeInbound(connectionId, message));
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }
}
