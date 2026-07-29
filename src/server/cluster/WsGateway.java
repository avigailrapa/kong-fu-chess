package src.server.cluster;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import src.net.MalformedMessageException;
import src.net.Protocol;
import src.net.messages.CancelPlayCommand;
import src.net.messages.LoginCommand;
import src.net.messages.MoveRejected;
import src.net.messages.PlayCommand;
import src.net.messages.RoomCreateCommand;
import src.net.messages.RoomJoinCommand;
import src.net.messages.WireMessage;
import src.server.coordinator.ConnectionDirectory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WsGateway extends WebSocketServer {

    private final Connection nats;
    private final ConnectionDirectory directory;
    private final Map<WebSocket, String> connectionIdsByConn = new ConcurrentHashMap<>();
    private final Map<String, WebSocket> connsByConnectionId = new ConcurrentHashMap<>();

    public WsGateway(InetSocketAddress address, Connection nats, ConnectionDirectory directory) {
        super(address);
        this.nats = nats;
        this.directory = directory;
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
        if (connectionId == null) {
            return;
        }
        connsByConnectionId.remove(connectionId);
        String subject = directory.nodeForConnection(connectionId)
                .map(ClusterProtocol::nodeDisconnectSubject)
                .orElse(ClusterProtocol.LOBBY_DISCONNECT_SUBJECT);
        nats.publish(subject, connectionId.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String connectionId = connectionIdsByConn.get(conn);
        if (connectionId == null) {
            return;
        }
        WireMessage parsed;
        try {
            parsed = Protocol.parse(message);
        } catch (MalformedMessageException e) {
            conn.send(Protocol.encode(new MoveRejected("malformed")));
            return;
        }
        if (isLobbyMessage(parsed)) {
            nats.publish(ClusterProtocol.LOBBY_INBOUND_SUBJECT, ClusterProtocol.encodeInbound(connectionId, message));
            return;
        }
        Optional<String> nodeId = directory.nodeForConnection(connectionId);
        if (nodeId.isEmpty()) {
            conn.send(Protocol.encode(new MoveRejected("not_in_match")));
            return;
        }
        nats.publish(ClusterProtocol.nodeInboundSubject(nodeId.get()), ClusterProtocol.encodeInbound(connectionId, message));
    }

    private static boolean isLobbyMessage(WireMessage parsed) {
        return parsed instanceof LoginCommand || parsed instanceof PlayCommand || parsed instanceof CancelPlayCommand
                || parsed instanceof RoomCreateCommand || parsed instanceof RoomJoinCommand;
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }
}
