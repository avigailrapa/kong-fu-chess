package src.server.handlers;

import src.engine.GameEngine;
import src.engine.GameOverEvent;
import src.engine.MoveEvent;
import src.io.BoardParser;
import src.model.Board;
import src.model.Piece;
import src.net.Protocol;
import src.net.messages.CancelPlayCommand;
import src.net.messages.MatchFound;
import src.net.messages.MatchTimeout;
import src.net.messages.MoveAccepted;
import src.net.messages.MoveRejected;
import src.net.messages.NewGameCommand;
import src.net.messages.PlayCommand;
import src.net.messages.RoomCreateCommand;
import src.net.messages.RoomId;
import src.net.messages.RoomJoinCommand;
import src.net.messages.Spectating;
import src.server.core.ActivityLog;
import src.server.core.Match;
import src.server.core.Session;
import src.server.core.SessionRegistry;
import src.server.matchmaking.MatchmakingQueue;
import src.server.matchmaking.RoomRegistry;

import java.util.List;
import java.util.function.Function;

public class MatchOrchestrator {

    private static final long MATCHMAKING_TIMEOUT_MS = 60_000;
    private static final int RATING_WINDOW = 100;

    private final long tickIntervalMs;
    private final ActivityLog activityLog;
    private final SessionRegistry sessionRegistry;
    private final MatchBroadcaster broadcaster;
    private final RatingService ratingService;
    private final MatchmakingQueue matchmakingQueue;
    private final RoomRegistry roomRegistry;

    public MatchOrchestrator(long tickIntervalMs, ActivityLog activityLog, SessionRegistry sessionRegistry,
                              MatchBroadcaster broadcaster, RatingService ratingService) {
        this.tickIntervalMs = tickIntervalMs;
        this.activityLog = activityLog;
        this.sessionRegistry = sessionRegistry;
        this.broadcaster = broadcaster;
        this.ratingService = ratingService;
        this.matchmakingQueue = new MatchmakingQueue(this::onPaired, this::onMatchTimeout,
                MATCHMAKING_TIMEOUT_MS, RATING_WINDOW);
        this.roomRegistry = new RoomRegistry(this::newMatch, this::seat, this::wireAndStartMatch, this::addSpectator);
    }

    public void cancelMatchmaking(Session session) {
        matchmakingQueue.cancel(session);
    }

    public String handlePlay(Object conn, PlayCommand p) {
        return requireSeatableSession(conn, session -> {
            matchmakingQueue.enqueue(session);
            return Protocol.encode(new MoveAccepted());
        });
    }

    public String handleCancelPlay(Object conn, CancelPlayCommand c) {
        Session session = sessionRegistry.sessionFor(conn);
        if (session != null) {
            matchmakingQueue.cancel(session);
        }
        return Protocol.encode(new MoveAccepted());
    }

    public String handleRoomCreate(Object conn, RoomCreateCommand r) {
        return requireSeatableSession(conn, session -> {
            String roomId = roomRegistry.createRoom(session);
            activityLog.log(session.username() + " created room " + roomId);
            return Protocol.encode(new RoomId(roomId));
        });
    }

    public String handleRoomJoin(Object conn, RoomJoinCommand r) {
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

    public String handleNewGame(Object conn, NewGameCommand ng) {
        return requireSession(conn, session -> {
            if (session.role() == Session.Role.SPECTATOR) {
                return Protocol.encode(new MoveRejected("spectator"));
            }
            Match match = sessionRegistry.matchFor(session);
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
        match.start(() -> broadcaster.broadcastState(match));
        List<Session> seated = match.seated();
        Session a = seated.get(0);
        Session b = seated.get(1);
        activityLog.log(a.username() + " vs " + b.username() + " - match started");
        broadcaster.sendQuietly(a, Protocol.encode(new MatchFound(b.username(), a.assignedColor(), b.rating())));
        broadcaster.sendQuietly(b, Protocol.encode(new MatchFound(a.username(), b.assignedColor(), a.rating())));
    }

    private void seat(Match match, Session session) {
        Piece.Color color = match.assignSeat().orElseThrow();
        session.assignedColor(color);
        session.role(color == Piece.Color.WHITE ? Session.Role.WHITE : Session.Role.BLACK);
        match.addSession(session);
        sessionRegistry.bind(session, match);
    }

    private void addSpectator(Match match, Session session) {
        session.role(Session.Role.SPECTATOR);
        match.addSpectator(session);
        sessionRegistry.bind(session, match);
    }

    private void onMatchTimeout(Session session) {
        broadcaster.sendQuietly(session, Protocol.encode(new MatchTimeout()));
    }

    private void subscribeToEngineEvents(Match match) {
        match.engine().eventBus().subscribe(MoveEvent.class, event -> broadcaster.broadcastMoveEvent(match, event));
        match.engine().eventBus().subscribe(GameOverEvent.class, event -> broadcaster.broadcastGameOver(match, event));
        match.engine().eventBus().subscribe(GameOverEvent.class, event -> ratingService.updateRatingsAfterGameOver(match, event));
        match.onNewGame(() -> subscribeToEngineEvents(match));
    }

    private String requireSeatableSession(Object conn, Function<Session, String> onEligible) {
        return requireSession(conn, session -> sessionRegistry.isBound(session)
                ? Protocol.encode(new MoveRejected("already_in_match"))
                : onEligible.apply(session));
    }

    private String requireSession(Object conn, Function<Session, String> onPresent) {
        Session session = sessionRegistry.sessionFor(conn);
        return session == null ? Protocol.encode(new MoveRejected("not_logged_in")) : onPresent.apply(session);
    }
}
