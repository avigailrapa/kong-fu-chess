package src.server;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import src.model.*;
import src.engine.GameEngine;
import src.engine.GameOverEvent;
import src.engine.MoveEvent;
import src.engine.MoveResult;
import src.io.BoardParser;
import src.net.GameOverMessage;
import src.net.JumpCommand;
import src.net.LoginCommand;
import src.net.MalformedMessageException;
import src.net.MoveAccepted;
import src.net.MoveCommand;
import src.net.MoveOccurred;
import src.net.MoveRejected;
import src.net.NewGameCommand;
import src.net.Protocol;
import src.net.RatingChanged;
import src.net.SelectCommand;
import src.net.StateMessage;
import src.net.Welcome;
import src.net.WireMessage;
import src.view.GameSnapshot;
import src.view.PieceSnapshot;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GameServer extends WebSocketServer {

    private final Match match;
    private final UserStore userStore;
    private final Map<WebSocket, Session> sessionsByConnection = new HashMap<>();

    public GameServer(InetSocketAddress address, Match match, UserStore userStore) {
        super(address);
        this.match = match;
        this.userStore = userStore;
        subscribeToEngineEvents();
        match.onNewGame(this::subscribeToEngineEvents);
    }

    private void subscribeToEngineEvents() {
        match.engine().eventBus().subscribe(MoveEvent.class, this::broadcastMoveEvent);
        match.engine().eventBus().subscribe(GameOverEvent.class, this::broadcastGameOver);
        match.engine().eventBus().subscribe(GameOverEvent.class, this::updateRatingsAfterGameOver);
    }

    private void broadcastMoveEvent(MoveEvent event) {
        broadcast(Protocol.encode(new MoveOccurred(event)));
    }

    private void broadcastGameOver(GameOverEvent event) {
        broadcast(Protocol.encode(new GameOverMessage(event)));
    }

    private void updateRatingsAfterGameOver(GameOverEvent event) {
        List<Session> seated = match.seated();
        Session white = seated.stream().filter(s -> s.assignedColor() == Piece.Color.WHITE).findFirst().orElse(null);
        Session black = seated.stream().filter(s -> s.assignedColor() == Piece.Color.BLACK).findFirst().orElse(null);
        if (white == null || black == null) {
            return;
        }
        double whiteScore = event.winner() == null ? 0.5 : event.winner() == Piece.Color.WHITE ? 1.0 : 0.0;
        int newWhiteRating = EloCalculator.updatedRating(white.rating(), black.rating(), whiteScore);
        int newBlackRating = EloCalculator.updatedRating(black.rating(), white.rating(), 1.0 - whiteScore);
        userStore.updateRating(white.username(), newWhiteRating);
        userStore.updateRating(black.username(), newBlackRating);
        white.rating(newWhiteRating);
        black.rating(newBlackRating);
        white.connection().send(Protocol.encode(new RatingChanged(newWhiteRating)));
        black.connection().send(Protocol.encode(new RatingChanged(newBlackRating)));
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
            case NewGameCommand ng -> handleNewGame(conn, ng);
            case MoveAccepted _ -> Protocol.encode(new MoveRejected("unexpected_message"));
            case MoveRejected _ -> Protocol.encode(new MoveRejected("unexpected_message"));
            case StateMessage _ -> Protocol.encode(new MoveRejected("unexpected_message"));
            case Welcome _ -> Protocol.encode(new MoveRejected("unexpected_message"));
            case MoveOccurred _ -> Protocol.encode(new MoveRejected("unexpected_message"));
            case GameOverMessage _ -> Protocol.encode(new MoveRejected("unexpected_message"));
            case RatingChanged _ -> Protocol.encode(new MoveRejected("unexpected_message"));
        };
    }

    private String handleSelect(WebSocket conn, SelectCommand sel) {
        Session session = sessionsByConnection.get(conn);
        if (session != null) {
            session.selectedCell(sel.selected());
        }
        return Protocol.encode(new MoveAccepted());
    }

    private String handleNewGame(WebSocket conn, NewGameCommand ng) {
        if (!sessionsByConnection.containsKey(conn)) {
            return Protocol.encode(new MoveRejected("not_logged_in"));
        }
        if (!match.engine().snapshot(null).gameOver()) {
            return Protocol.encode(new MoveRejected("game_in_progress"));
        }
        Board board = new BoardParser().parse(BoardParser.STANDARD_STARTING_POSITION);
        match.newGame(GameEngine.fromBoard(board));
        return Protocol.encode(new MoveAccepted());
    }

    private String handleLogin(WebSocket conn, LoginCommand l) {
        Optional<Piece.Color> color = match.assignSeat();
        if (color.isEmpty()) {
            return Protocol.encode(new MoveRejected("table_full"));
        }
        Optional<UserRecord> existing = userStore.find(l.username());
        UserRecord user;
        if (existing.isPresent()) {
            if (!userStore.checkPassword(l.username(), l.password())) {
                return Protocol.encode(new MoveRejected("bad_credentials"));
            }
            user = existing.get();
        } else {
            user = userStore.createUser(l.username(), l.password());
        }
        Session session = new Session(conn::send, l.username(), color.get(), user.rating());
        match.addSession(session);
        sessionsByConnection.put(conn, session);
        return Protocol.encode(new Welcome(color.get(), user.rating()));
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
