package src.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import src.model.*;
import src.engine.MoveEvent;
import src.engine.MoveResult;
import src.net.JumpCommand;
import src.net.MalformedMessageException;
import src.net.MoveAccepted;
import src.net.MoveCommand;
import src.net.MoveRejected;
import src.net.Protocol;
import src.net.StateMessage;
import src.net.WireMessage;
import src.view.GameSnapshot;
import src.view.PieceSnapshot;

import java.net.InetSocketAddress;

public class GameServer extends WebSocketServer {

    private final Match match;

    public GameServer(InetSocketAddress address, Match match) {
        super(address);
        this.match = match;
    }

    @Override
    public void onStart() {
        match.start(this::broadcastState);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        match.submit(() -> conn.send(encodedState()));
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        match.submit(() -> {
            String reply = handleMessage(message);
            conn.send(reply);
            broadcastState();
        });
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    public String handleMessage(String message) {
        WireMessage parsed;
        try {
            parsed = Protocol.parse(message);
        } catch (MalformedMessageException e) {
            return Protocol.encode(new MoveRejected("malformed"));
        }
        return switch (parsed) {
            case MoveCommand m -> handleMove(m);
            case JumpCommand j -> handleJump(j);
            case MoveAccepted _ -> Protocol.encode(new MoveRejected("unexpected_message"));
            case MoveRejected _ -> Protocol.encode(new MoveRejected("unexpected_message"));
            case StateMessage _ -> Protocol.encode(new MoveRejected("unexpected_message"));
        };
    }

    private String handleMove(MoveCommand m) {
        if (!declaredTokenMatchesBoard(m.from(), m.color(), m.kind())) {
            return Protocol.encode(new MoveRejected("token_mismatch"));
        }
        MoveResult result = match.engine().requestMove(m.from(), m.to());
        return result.isAccepted()
                ? Protocol.encode(new MoveAccepted())
                : Protocol.encode(new MoveRejected(result.reason()));
    }

    private String handleJump(JumpCommand j) {
        if (!declaredTokenMatchesBoard(j.at(), j.color(), j.kind())) {
            return Protocol.encode(new MoveRejected("token_mismatch"));
        }
        match.engine().requestJump(j.at());
        return Protocol.encode(new MoveAccepted());
    }

    private boolean declaredTokenMatchesBoard(Position position, Piece.Color color,
                                              Piece.Kind kind) {
        PieceSnapshot piece = match.engine().snapshot(null).pieceAt(position);
        return piece != null && piece.color() == color && piece.kind() == kind;
    }

    private void broadcastState() {
        broadcast(encodedState());
    }

    private String encodedState() {
        GameSnapshot snapshot = match.engine().snapshot(null,
                match.moveLogger().getWhiteMoves().stream().map(MoveEvent::toString).toList(),
                match.moveLogger().getBlackMoves().stream().map(MoveEvent::toString).toList());
        return Protocol.encode(new StateMessage(snapshot));
    }
}
