package src.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import src.server.auth.UserStore;

import java.net.InetSocketAddress;

public class GameServer extends WebSocketServer {

    private final Lobby lobby;

    public GameServer(InetSocketAddress address, UserStore userStore, long tickIntervalMs,
                       int disconnectCountdownSeconds, ActivityLog activityLog) {
        super(address);
        this.lobby = new Lobby(userStore, tickIntervalMs, disconnectCountdownSeconds, activityLog);
    }

    public Match matchFor(WebSocket conn) {
        return lobby.matchFor(conn);
    }

    public String handleMessage(WebSocket conn, String message) {
        return lobby.handleMessage(conn, message);
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        lobby.disconnect(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        lobby.receive(conn, message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }
}
