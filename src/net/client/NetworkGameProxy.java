package src.net.client;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import src.bus.EventBus;
import src.engine.GameCommands;
import src.engine.MoveResult;
import src.model.Position;
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
import src.view.GameSnapshot;
import src.view.PieceSnapshot;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class NetworkGameProxy extends WebSocketClient implements GameCommands {

    private final long requestTimeoutMs;
  
    private final LinkedBlockingQueue<CompletableFuture<WireMessage>> pendingReplies = new LinkedBlockingQueue<>();
    @Getter
    @Accessors(fluent = true)
    private volatile GameSnapshot latestSnapshot;
    @Getter
    @Accessors(fluent = true)
    private final EventBus eventBus = new EventBus();
    @Getter
    @Accessors(fluent = true)
    private volatile int latestRating;

    private final ClientActivityLog activityLog;

    public NetworkGameProxy(URI serverUri, long requestTimeoutMs) {
        this(serverUri, requestTimeoutMs, null);
    }

    public NetworkGameProxy(URI serverUri, long requestTimeoutMs, ClientActivityLog activityLog) {
        super(serverUri);
        this.requestTimeoutMs = requestTimeoutMs;
        this.activityLog = activityLog;
    }

    @Override
    public void send(String text) {
        logDirection("CLIENT_TO_SERVER", text);
        super.send(text);
    }

    private void logDirection(String direction, String text) {
        if (activityLog != null && !text.startsWith("STATE ")) {
            activityLog.log(direction + " " + text);
        }
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
    }

    @Override
    public void onMessage(String message) {
        logDirection("SERVER_TO_CLIENT", message);
        WireMessage parsed;
        try {
            parsed = Protocol.parse(message);
        } catch (MalformedMessageException e) {
            return;
        }
        switch (parsed) {
            case StateMessage s -> latestSnapshot = s.snapshot();
            case MoveAccepted m -> completeOldestPendingReply(m);
            case MoveRejected r -> completeOldestPendingReply(r);
            case Welcome w -> {
                latestRating = w.rating();
                completeOldestPendingReply(w);
            }
            case WelcomeBack w -> {
                latestRating = w.rating();
                completeOldestPendingReply(w);
            }
            case OpponentReconnected r -> eventBus.publish(r);
            case MoveOccurred mo -> eventBus.publish(mo.event());
            case GameOverMessage go -> eventBus.publish(go.event());
            case RatingChanged r -> {
                latestRating = r.newRating();
                eventBus.publish(r);
            }
            case MatchFound mf -> eventBus.publish(mf);
            case MatchTimeout mt -> eventBus.publish(mt);
            case DisconnectCountdown dc -> eventBus.publish(dc);
            case RoomId r -> completeOldestPendingReply(r);
            case Spectating s -> completeOldestPendingReply(s);
            case LoginCommand _ -> {
            }
            case PlayCommand _ -> {
            }
            case CancelPlayCommand _ -> {
            }
            case MoveCommand _ -> {
            }
            case JumpCommand _ -> {
            }
            case SelectCommand _ -> {
            }
            case NewGameCommand _ -> {
            }
            case RoomCreateCommand _ -> {
            }
            case RoomJoinCommand _ -> {
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public boolean isOccupied(Position position) {
        return latestSnapshot != null && latestSnapshot.isOccupied(position);
    }

    public MoveResult requestMove(Position source, Position destination) {
        PieceSnapshot piece = pieceAt(source);
        if (piece == null) {
            return new MoveResult(false, "empty_source");
        }
        CompletableFuture<WireMessage> reply = enqueuePendingReply();
        send(Protocol.encode(new MoveCommand(piece.color(), piece.kind(), source, destination)));
        return awaitMoveReply(reply);
    }

    @Override
    public void requestMove(Position source, Position destination, Consumer<MoveResult> onResult) {
        PieceSnapshot piece = pieceAt(source);
        if (piece == null) {
            onResult.accept(new MoveResult(false, "empty_source"));
            return;
        }
        CompletableFuture<WireMessage> reply = enqueuePendingReply();
        send(Protocol.encode(new MoveCommand(piece.color(), piece.kind(), source, destination)));
        reply.orTimeout(requestTimeoutMs, TimeUnit.MILLISECONDS)
                .whenComplete((message, error) -> onResult.accept(toAsyncMoveResult(message, error)));
    }

    private MoveResult toAsyncMoveResult(WireMessage message, Throwable error) {
        if (error instanceof TimeoutException) {
            return new MoveResult(false, "timeout");
        }
        if (error != null) {
            return new MoveResult(false, "error");
        }
        return toMoveResult(message);
    }

    @Override
    public void requestJump(Position cell) {
        PieceSnapshot piece = pieceAt(cell);
        if (piece == null) {
            return;
        }
        enqueuePendingReply();
        send(Protocol.encode(new JumpCommand(piece.color(), piece.kind(), cell)));
    }

    public LoginResult login(String username, String password) {
        CompletableFuture<WireMessage> reply = enqueuePendingReply();
        send(Protocol.encode(new LoginCommand(username, password)));
        return awaitLoginReply(reply);
    }

    public void updateSelection(Position selected) {
        enqueuePendingReply();
        send(Protocol.encode(new SelectCommand(selected)));
    }

    public void newGame() {
        enqueuePendingReply();
        send(Protocol.encode(new NewGameCommand()));
    }

    public void play() {
        enqueuePendingReply();
        send(Protocol.encode(new PlayCommand()));
    }

    public void cancelPlay() {
        enqueuePendingReply();
        send(Protocol.encode(new CancelPlayCommand()));
    }

    public void resetSnapshot() {
        latestSnapshot = null;
    }

    public RoomCreateResult createRoom() {
        CompletableFuture<WireMessage> reply = enqueuePendingReply();
        send(Protocol.encode(new RoomCreateCommand()));
        return awaitRoomCreateReply(reply);
    }

    public RoomJoinResult joinRoom(String roomId) {
        CompletableFuture<WireMessage> reply = enqueuePendingReply();
        send(Protocol.encode(new RoomJoinCommand(roomId)));
        return awaitRoomJoinReply(reply);
    }

    private PieceSnapshot pieceAt(Position position) {
        return latestSnapshot == null ? null : latestSnapshot.pieceAt(position);
    }

    private CompletableFuture<WireMessage> enqueuePendingReply() {
        CompletableFuture<WireMessage> reply = new CompletableFuture<>();
        pendingReplies.add(reply);
        return reply;
    }

    private void completeOldestPendingReply(WireMessage result) {
        CompletableFuture<WireMessage> reply = pendingReplies.poll();
        if (reply != null) {
            reply.complete(result);
        }
    }

    private MoveResult awaitMoveReply(CompletableFuture<WireMessage> reply) {
        try {
            return toMoveResult(reply.get(requestTimeoutMs, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            return new MoveResult(false, "timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new MoveResult(false, "interrupted");
        } catch (ExecutionException e) {
            return new MoveResult(false, "error");
        }
    }

    private MoveResult toMoveResult(WireMessage message) {
        return switch (message) {
            case MoveAccepted _ -> new MoveResult(true, "ok");
            case MoveRejected r -> new MoveResult(false, r.reason());
            default -> new MoveResult(false, "unexpected_message");
        };
    }

    private LoginResult awaitLoginReply(CompletableFuture<WireMessage> reply) {
        try {
            return toLoginResult(reply.get(requestTimeoutMs, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            return new LoginResult(false, 0, "timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new LoginResult(false, 0, "interrupted");
        } catch (ExecutionException e) {
            return new LoginResult(false, 0, "error");
        }
    }

    private LoginResult toLoginResult(WireMessage message) {
        return switch (message) {
            case Welcome w -> new LoginResult(true, w.rating(), "ok");
            case WelcomeBack w -> new LoginResult(true, w.rating(), "reconnected");
            case MoveRejected r -> new LoginResult(false, 0, r.reason());
            default -> new LoginResult(false, 0, "unexpected_message");
        };
    }

    private RoomCreateResult awaitRoomCreateReply(CompletableFuture<WireMessage> reply) {
        try {
            return toRoomCreateResult(reply.get(requestTimeoutMs, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            return new RoomCreateResult(false, null, "timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new RoomCreateResult(false, null, "interrupted");
        } catch (ExecutionException e) {
            return new RoomCreateResult(false, null, "error");
        }
    }

    private RoomCreateResult toRoomCreateResult(WireMessage message) {
        return switch (message) {
            case RoomId r -> new RoomCreateResult(true, r.roomId(), "ok");
            case MoveRejected r -> new RoomCreateResult(false, null, r.reason());
            default -> new RoomCreateResult(false, null, "unexpected_message");
        };
    }

    private RoomJoinResult awaitRoomJoinReply(CompletableFuture<WireMessage> reply) {
        try {
            return toRoomJoinResult(reply.get(requestTimeoutMs, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            return new RoomJoinResult(false, false, "timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new RoomJoinResult(false, false, "interrupted");
        } catch (ExecutionException e) {
            return new RoomJoinResult(false, false, "error");
        }
    }

    private RoomJoinResult toRoomJoinResult(WireMessage message) {
        return switch (message) {
            case RoomId _ -> new RoomJoinResult(true, false, "ok");
            case Spectating _ -> new RoomJoinResult(true, true, "ok");
            case MoveRejected r -> new RoomJoinResult(false, false, r.reason());
            default -> new RoomJoinResult(false, false, "unexpected_message");
        };
    }
}
