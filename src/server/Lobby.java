package src.server;

import org.java_websocket.WebSocket;

import src.model.*;
import src.engine.GameEngine;
import src.engine.GameOverEvent;
import src.engine.MoveEvent;
import src.engine.MoveResult;
import src.io.BoardParser;
import src.net.MalformedMessageException;
import src.net.Protocol;
import src.net.messages.CancelPlayCommand;
import src.net.messages.DisconnectCountdown;
import src.net.messages.GameOverMessage;
import src.net.messages.JumpCommand;
import src.net.messages.LoginCommand;
import src.net.messages.MatchFound;
import src.net.messages.MatchTimeout;
import src.net.messages.MoveAccepted;
import src.net.messages.MoveCommand;
import src.net.messages.MoveOccurred;
import src.net.messages.MoveRejected;
import src.net.messages.NewGameCommand;
import src.net.messages.OpponentReconnected;
import src.net.messages.PlayCommand;
import src.net.messages.RatingChanged;
import src.net.messages.RoomCreateCommand;
import src.net.messages.RoomId;
import src.net.messages.RoomJoinCommand;
import src.net.messages.SelectCommand;
import src.net.messages.Spectating;
import src.net.messages.StateMessage;
import src.net.messages.Welcome;
import src.net.messages.WelcomeBack;
import src.net.messages.WireMessage;
import src.server.auth.UserRecord;
import src.server.auth.UserStore;
import src.server.matchmaking.MatchmakingQueue;
import src.server.matchmaking.ReconnectManager;
import src.server.matchmaking.RoomRegistry;
import src.view.snapshot.GameSnapshot;
import src.view.snapshot.PieceSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class Lobby {

    private static final long MATCHMAKING_TIMEOUT_MS = 60_000;
    private static final int RATING_WINDOW = 100;

    private final UserStore userStore;
    private final long tickIntervalMs;
    private final ActivityLog activityLog;
    private final MatchmakingQueue matchmakingQueue;
    private final RoomRegistry roomRegistry;
    private final ReconnectManager reconnectManager;
    private final Map<WebSocket, Session> sessionsByConnection = new HashMap<>();
    private final Map<Session, Match> matchBySession = new HashMap<>();

    public Lobby(UserStore userStore, long tickIntervalMs, int disconnectCountdownSeconds, ActivityLog activityLog) {
        this.userStore = userStore;
        this.tickIntervalMs = tickIntervalMs;
        this.activityLog = activityLog;
        this.matchmakingQueue = new MatchmakingQueue(this::onPaired, this::onMatchTimeout,
                MATCHMAKING_TIMEOUT_MS, RATING_WINDOW);
        this.roomRegistry = new RoomRegistry(this::newMatch, this::seat, this::wireAndStartMatch, this::addSpectator);
        this.reconnectManager = new ReconnectManager(disconnectCountdownSeconds);
    }

    public Match matchFor(WebSocket conn) {
        Session session = sessionsByConnection.get(conn);
        return session == null ? null : matchBySession.get(session);
    }

    public void receive(WebSocket conn, String message) {
        activityLog.log("CLIENT_TO_SERVER " + message);
        Match match = matchFor(conn);
        Runnable task = () -> {
            String reply = handleMessage(conn, message);
            conn.send(reply);
            activityLog.log("SERVER_TO_CLIENT " + reply);
            if (match != null) {
                broadcastState(match);
            }
        };
        if (match != null) {
            match.submit(task);
        } else {
            task.run();
        }
    }

    public void disconnect(WebSocket conn) {
        Session session = sessionsByConnection.remove(conn);
        if (session == null) {
            return;
        }
        matchmakingQueue.cancel(session);
        Match match = matchBySession.get(session);
        if (match != null && match.seated().contains(session)) {
            startDisconnectCountdown(match, session);
        }
    }

    private Match newMatch() {
        return new Match(freshEngine(), tickIntervalMs);
    }

    private GameEngine freshEngine() {
        Board board = new BoardParser().parse(BoardParser.STANDARD_STARTING_POSITION);
        return GameEngine.fromBoard(board);
    }

    private void onPaired(Session a, Session b) {
        Match match = newMatch();
        seat(match, a);
        seat(match, b);
        wireAndStartMatch(match);
    }

    private void wireAndStartMatch(Match match) {
        subscribeToEngineEvents(match);
        match.start(() -> broadcastState(match));
        List<Session> seated = match.seated();
        Session a = seated.get(0);
        Session b = seated.get(1);
        activityLog.log(a.username() + " vs " + b.username() + " - match started");
        sendQuietly(a, Protocol.encode(new MatchFound(b.username(), a.assignedColor(), b.rating())));
        sendQuietly(b, Protocol.encode(new MatchFound(a.username(), b.assignedColor(), a.rating())));
    }

    private void seat(Match match, Session session) {
        Piece.Color color = match.assignSeat().orElseThrow();
        session.assignedColor(color);
        session.role(color == Piece.Color.WHITE ? Session.Role.WHITE : Session.Role.BLACK);
        match.addSession(session);
        matchBySession.put(session, match);
    }

    private void addSpectator(Match match, Session session) {
        session.role(Session.Role.SPECTATOR);
        match.addSpectator(session);
        matchBySession.put(session, match);
    }

    private void onMatchTimeout(Session session) {
        sendQuietly(session, Protocol.encode(new MatchTimeout()));
    }

    private void subscribeToEngineEvents(Match match) {
        match.engine().eventBus().subscribe(MoveEvent.class, event -> broadcastMoveEvent(match, event));
        match.engine().eventBus().subscribe(GameOverEvent.class, event -> broadcastGameOver(match, event));
        match.engine().eventBus().subscribe(GameOverEvent.class, event -> updateRatingsAfterGameOver(match, event));
        match.onNewGame(() -> subscribeToEngineEvents(match));
    }

    private void broadcastMoveEvent(Match match, MoveEvent event) {
        broadcastToMatch(match, Protocol.encode(new MoveOccurred(event)));
    }

    private void broadcastGameOver(Match match, GameOverEvent event) {
        activityLog.log("game over - winner: " + (event.winner() == null ? "draw" : event.winner()));
        broadcastToMatch(match, Protocol.encode(new GameOverMessage(event)));
    }

    private void broadcastToMatch(Match match, String text) {
        for (Session session : match.seated()) {
            sendQuietly(session, text);
        }
        for (Session spectator : match.spectators()) {
            sendQuietly(spectator, text);
        }
    }

    private void sendQuietly(Session session, String text) {
        logOutgoing(text);
        try {
            session.connection().send(text);
        } catch (RuntimeException e) {
        }
    }

    private void logOutgoing(String text) {
        if (!text.startsWith("STATE ")) {
            activityLog.log("SERVER_TO_CLIENT " + text);
        }
    }

    private void updateRatingsAfterGameOver(Match match, GameOverEvent event) {
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
        sendQuietly(white, Protocol.encode(new RatingChanged(newWhiteRating)));
        sendQuietly(black, Protocol.encode(new RatingChanged(newBlackRating)));
    }

    private void startDisconnectCountdown(Match match, Session disconnected) {
        Session opponent = match.seated().stream().filter(s -> s != disconnected).findFirst().orElse(null);
        if (opponent == null) {
            return;
        }
        activityLog.log(disconnected.username() + " disconnected - starting resign countdown");
        reconnectManager.startCountdown(match, disconnected,
                secondsRemaining -> sendQuietly(opponent, Protocol.encode(new DisconnectCountdown(secondsRemaining))),
                () -> {
                    activityLog.log(disconnected.username() + " did not reconnect - auto-resigning");
                    match.submit(() -> match.engine().resign(disconnected.assignedColor()));
                });
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
            case PlayCommand p -> handlePlay(conn, p);
            case CancelPlayCommand c -> handleCancelPlay(conn, c);
            case MoveCommand m -> handleMove(conn, m);
            case JumpCommand j -> handleJump(conn, j);
            case SelectCommand sel -> handleSelect(conn, sel);
            case NewGameCommand ng -> handleNewGame(conn, ng);
            case RoomCreateCommand r -> handleRoomCreate(conn, r);
            case RoomJoinCommand r -> handleRoomJoin(conn, r);
            default -> Protocol.encode(new MoveRejected("unexpected_message"));
        };
    }

    private String handleRoomCreate(WebSocket conn, RoomCreateCommand r) {
        return requireSeatableSession(conn, session -> {
            String roomId = roomRegistry.createRoom(session);
            activityLog.log(session.username() + " created room " + roomId);
            return Protocol.encode(new RoomId(roomId));
        });
    }

    private String handleRoomJoin(WebSocket conn, RoomJoinCommand r) {
        return requireSeatableSession(conn, session -> {
            RoomRegistry.JoinOutcome outcome = roomRegistry.joinRoom(r.roomId(), session);
            return switch (outcome) {
                case SEATED_BLACK -> {
                    activityLog.log(session.username() + " joined room " + r.roomId() + " as black");
                    yield Protocol.encode(new RoomId(r.roomId()));
                }
                case SPECTATING -> {
                    activityLog.log(session.username() + " is spectating room " + r.roomId());
                    yield Protocol.encode(new Spectating());
                }
                case NOT_FOUND -> Protocol.encode(new MoveRejected("room_not_found"));
            };
        });
    }

    private String handlePlay(WebSocket conn, PlayCommand p) {
        return requireSeatableSession(conn, session -> {
            matchmakingQueue.enqueue(session);
            return Protocol.encode(new MoveAccepted());
        });
    }

    private String requireSeatableSession(WebSocket conn, Function<Session, String> onEligible) {
        return requireSession(conn, session -> matchBySession.containsKey(session)
                ? Protocol.encode(new MoveRejected("already_in_match"))
                : onEligible.apply(session));
    }

    private String requireSession(WebSocket conn, Function<Session, String> onPresent) {
        Session session = sessionsByConnection.get(conn);
        return session == null ? Protocol.encode(new MoveRejected("not_logged_in")) : onPresent.apply(session);
    }

    private String handleCancelPlay(WebSocket conn, CancelPlayCommand c) {
        Session session = sessionsByConnection.get(conn);
        if (session != null) {
            matchmakingQueue.cancel(session);
        }
        return Protocol.encode(new MoveAccepted());
    }

    private String handleSelect(WebSocket conn, SelectCommand sel) {
        Session session = sessionsByConnection.get(conn);
        if (session != null) {
            session.selectedCell(sel.selected());
        }
        return Protocol.encode(new MoveAccepted());
    }

    private String handleNewGame(WebSocket conn, NewGameCommand ng) {
        return requireSession(conn, session -> {
            if (session.role() == Session.Role.SPECTATOR) {
                return Protocol.encode(new MoveRejected("spectator"));
            }
            Match match = matchBySession.get(session);
            if (match == null) {
                return Protocol.encode(new MoveRejected("not_in_match"));
            }
            if (!match.engine().snapshot(null).gameOver()) {
                return Protocol.encode(new MoveRejected("game_in_progress"));
            }
            match.newGame(freshEngine());
            return Protocol.encode(new MoveAccepted());
        });
    }

    private String handleLogin(WebSocket conn, LoginCommand l) {
        Optional<ReconnectManager.Pending> pending = reconnectManager.pendingFor(l.username());
        if (pending.isPresent()) {
            if (!userStore.checkPassword(l.username(), l.password())) {
                activityLog.log(l.username() + " login rejected: bad_credentials");
                return Protocol.encode(new MoveRejected("bad_credentials"));
            }
            reconnectManager.cancelCountdown(l.username());
            return reconnectSession(conn, pending.get());
        }

        Optional<UserRecord> existing = userStore.find(l.username());
        UserRecord user;
        if (existing.isPresent()) {
            if (!userStore.checkPassword(l.username(), l.password())) {
                activityLog.log(l.username() + " login rejected: bad_credentials");
                return Protocol.encode(new MoveRejected("bad_credentials"));
            }
            user = existing.get();
        } else {
            user = userStore.createUser(l.username(), l.password());
        }
        Session session = new Session(conn::send, l.username(), user.rating());
        sessionsByConnection.put(conn, session);
        activityLog.log(l.username() + " logged in (rating " + user.rating() + ")");
        return Protocol.encode(new Welcome(user.rating()));
    }

    private String reconnectSession(WebSocket conn, ReconnectManager.Pending pending) {
        Session session = pending.session();
        session.connection(conn::send);
        sessionsByConnection.put(conn, session);
        activityLog.log(session.username() + " reconnected");
        Session opponent = pending.match().seated().stream().filter(s -> s != session).findFirst().orElse(null);
        if (opponent != null) {
            sendQuietly(opponent, Protocol.encode(new OpponentReconnected()));
        }
        return Protocol.encode(new WelcomeBack(session.rating()));
    }

    private String handleMove(WebSocket conn, MoveCommand m) {
        String rejection = validateSeatedAction(conn, m.color(), m.kind(), m.from());
        if (rejection != null) {
            return Protocol.encode(new MoveRejected(rejection));
        }
        MoveResult result = matchFor(conn).engine().requestMove(m.from(), m.to());
        return result.isAccepted()
                ? Protocol.encode(new MoveAccepted())
                : Protocol.encode(new MoveRejected(result.reason()));
    }

    private String handleJump(WebSocket conn, JumpCommand j) {
        String rejection = validateSeatedAction(conn, j.color(), j.kind(), j.at());
        if (rejection != null) {
            return Protocol.encode(new MoveRejected(rejection));
        }
        matchFor(conn).engine().requestJump(j.at());
        return Protocol.encode(new MoveAccepted());
    }

    private String validateSeatedAction(WebSocket conn, Piece.Color color, Piece.Kind kind, Position position) {
        if (isSpectator(conn)) {
            return "spectator";
        }
        Match match = matchFor(conn);
        if (match == null || !ownsColor(conn, color)) {
            return "not_your_piece";
        }
        if (!declaredTokenMatchesBoard(match, position, color, kind)) {
            return "token_mismatch";
        }
        return null;
    }

    private boolean ownsColor(WebSocket conn, Piece.Color declaredColor) {
        Session session = sessionsByConnection.get(conn);
        return session != null && session.assignedColor() == declaredColor;
    }

    private boolean isSpectator(WebSocket conn) {
        Session session = sessionsByConnection.get(conn);
        return session != null && session.role() == Session.Role.SPECTATOR;
    }

    private boolean declaredTokenMatchesBoard(Match match, Position position, Piece.Color color,
                                              Piece.Kind kind) {
        PieceSnapshot piece = match.engine().snapshot(null).pieceAt(position);
        return piece != null && piece.color() == color && piece.kind() == kind;
    }

    private void broadcastState(Match match) {
        GameSnapshot boardState = match.engine().snapshot(null);
        for (Session session : match.seated()) {
            sendQuietly(session, encodedState(match, ownSelection(session, boardState)));
        }
        for (Session spectator : match.spectators()) {
            sendQuietly(spectator, encodedState(match, null));
        }
    }

    private Position ownSelection(Session session, GameSnapshot boardState) {
        if (session == null || session.selectedCell() == null) {
            return null;
        }
        PieceSnapshot piece = boardState.pieceAt(session.selectedCell());
        return piece != null && piece.color() == session.assignedColor() ? session.selectedCell() : null;
    }

    private String encodedState(Match match, Position selectedCell) {
        GameSnapshot snapshot = match.engine().snapshot(selectedCell,
                match.moveLogger().whiteMoves().stream().map(MoveEvent::toString).toList(),
                match.moveLogger().blackMoves().stream().map(MoveEvent::toString).toList());
        return Protocol.encode(new StateMessage(snapshot));
    }
}
