package src.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import src.model.*;
import src.engine.GameOverEvent;
import src.engine.MoveEvent;
import src.engine.MoveResult;
import src.net.GameOverMessage;
import src.net.JumpCommand;
import src.net.LoginCommand;
import src.net.MalformedMessageException;
import src.net.MoveAccepted;
import src.net.MoveCommand;
import src.net.MoveOccurred;
import src.net.MoveRejected;
import src.net.Protocol;
import src.net.SelectCommand;
import src.net.StateMessage;
import src.net.Welcome;
import src.net.WireMessage;
import src.view.GameSnapshot;
import src.view.PieceSnapshot;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GameServer extends WebSocketServer {

    private final Match match;
    private final Map<WebSocket, Session> sessionsByConnection = new HashMap<>();

    public GameServer(InetSocketAddress address, Match match) {
        super(address);
        this.match = match;
        match.engine().eventBus().subscribe(MoveEvent.class, this::broadcastMoveEvent);
        match.engine().eventBus().subscribe(GameOverEvent.class, this::broadcastGameOver);
    }

    private void broadcastMoveEvent(MoveEvent event) {
        broadcast(Protocol.encode(new MoveOccurred(event)));
    }

    private void broadcastGameOver(GameOverEvent event) {
        broadcast(Protocol.encode(new GameOverMessage(event)));
    }

    @Override
    public void onStart() {
        match.start(this::broadcastState);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        match.submit(() -> {
            String reply = handleMessage(conn, message);
            conn.send(reply);
            broadcastState();
        });
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    public String handleMessage(WebSocket conn, String message) {
        WireMessage parsed;
        try {
            parsed = Protocol.parse(message);
        } catch (MalformedMessageException e) {
            return Protocol.encode(new MoveRejected("malformed"));
        }
        return switch (parsed) {
            case LoginCommand l -> handleLogin(conn, l);
            case MoveCommand m -> handleMove(conn, m);
            case JumpCommand j -> handleJump(conn, j);
            case SelectCommand sel -> handleSelect(conn, sel);
            case MoveAccepted _ -> Protocol.encode(new MoveRejected("unexpected_message"));
            case MoveRejected _ -> Protocol.encode(new MoveRejected("unexpected_message"));
            case StateMessage _ -> Protocol.encode(new MoveRejected("unexpected_message"));
            case Welcome _ -> Protocol.encode(new MoveRejected("unexpected_message"));
            case MoveOccurred _ -> Protocol.encode(new MoveRejected("unexpected_message"));
            case GameOverMessage _ -> Protocol.encode(new MoveRejected("unexpected_message"));
        };
    }

    private String handleSelect(WebSocket conn, SelectCommand sel) {
        Session session = sessionsByConnection.get(conn);
        if (session != null) {
            session.selectedCell(sel.selected());
        }
        return Protocol.encode(new MoveAccepted());
    }

    private String handleLogin(WebSocket conn, LoginCommand l) {
        Optional<Piece.Color> color = match.assignSeat();
        if (color.isEmpty()) {
            return Protocol.encode(new MoveRejected("table_full"));
        }
        Session session = new Session(conn, l.username(), color.get());
        match.addSession(session);
        sessionsByConnection.put(conn, session);
        return Protocol.encode(new Welcome(color.get()));
    }

    private String handleMove(WebSocket conn, MoveCommand m) {
        if (!ownsColor(conn, m.color())) {
            return Protocol.encode(new MoveRejected("not_your_piece"));
        }
        if (!declaredTokenMatchesBoard(m.from(), m.color(), m.kind())) {
            return Protocol.encode(new MoveRejected("token_mismatch"));
        }
        MoveResult result = match.engine().requestMove(m.from(), m.to());
        return result.isAccepted()
                ? Protocol.encode(new MoveAccepted())
                : Protocol.encode(new MoveRejected(result.reason()));
    }

    private String handleJump(WebSocket conn, JumpCommand j) {
        if (!ownsColor(conn, j.color())) {
            return Protocol.encode(new MoveRejected("not_your_piece"));
        }
        if (!declaredTokenMatchesBoard(j.at(), j.color(), j.kind())) {
            return Protocol.encode(new MoveRejected("token_mismatch"));
        }
        match.engine().requestJump(j.at());
        return Protocol.encode(new MoveAccepted());
    }

    private boolean ownsColor(WebSocket conn, Piece.Color declaredColor) {
        Session session = sessionsByConnection.get(conn);
        return session != null && session.assignedColor() == declaredColor;
    }

    private boolean declaredTokenMatchesBoard(Position position, Piece.Color color,
                                              Piece.Kind kind) {
        PieceSnapshot piece = match.engine().snapshot(null).pieceAt(position);
        return piece != null && piece.color() == color && piece.kind() == kind;
    }

    private void broadcastState() {
        GameSnapshot boardState = match.engine().snapshot(null);
        for (WebSocket conn : getConnections()) {
            Session session = sessionsByConnection.get(conn);
            conn.send(encodedState(ownSelection(session, boardState)));
        }
    }

    private Position ownSelection(Session session, GameSnapshot boardState) {
        if (session == null || session.selectedCell() == null) {
            return null;
        }
        PieceSnapshot piece = boardState.pieceAt(session.selectedCell());
        return piece != null && piece.color() == session.assignedColor() ? session.selectedCell() : null;
    }

    private String encodedState(Position selectedCell) {
        GameSnapshot snapshot = match.engine().snapshot(selectedCell,
                match.moveLogger().getWhiteMoves().stream().map(MoveEvent::toString).toList(),
                match.moveLogger().getBlackMoves().stream().map(MoveEvent::toString).toList());
        return Protocol.encode(new StateMessage(snapshot));
    }
}
