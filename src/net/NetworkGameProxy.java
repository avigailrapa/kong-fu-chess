package src.net;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import src.bus.EventBus;
import src.engine.GameCommands;
import src.engine.MoveResult;
import src.model.Position;
import src.view.GameSnapshot;
import src.view.PieceSnapshot;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NetworkGameProxy extends WebSocketClient implements GameCommands {

    private final long requestTimeoutMs;
    // A single WebSocket connection delivers frames in send order both ways, and GameServer replies to a
    // connection's messages in the order it received them, so the reply for each request we sent is always
    // the next one to arrive here. Matching by strict FIFO order (rather than a single shared slot) means a
    // late reply to a request we already gave up on (timeout) completes that abandoned future instead of
    // being mistaken for the reply to whatever request we send next. Holding the raw WireMessage (rather
    // than a MoveResult) lets this same queue carry login replies too, since login's WELCOME/REJECT and a
    // move's OK/REJECT are both just "whatever reply comes back next" from the server's point of view.
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

    public NetworkGameProxy(URI serverUri, long requestTimeoutMs) {
        super(serverUri);
        this.requestTimeoutMs = requestTimeoutMs;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
    }

    @Override
    public void onMessage(String message) {
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
            case MoveOccurred mo -> eventBus.publish(mo.event());
            case GameOverMessage go -> eventBus.publish(go.event());
            case RatingChanged r -> latestRating = r.newRating();
            case LoginCommand _ -> {
            }
            case MoveCommand _ -> {
            }
            case JumpCommand _ -> {
            }
            case SelectCommand _ -> {
            }
            case NewGameCommand _ -> {
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

    @Override
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
    public void requestJump(Position cell) {
        PieceSnapshot piece = pieceAt(cell);
        if (piece == null) {
            return;
        }
        // The server replies OK/REJECT to a jump too; enqueue a (never-awaited) placeholder so that
        // reply is consumed here rather than being mistaken for a later requestMove's reply.
        enqueuePendingReply();
        send(Protocol.encode(new JumpCommand(piece.color(), piece.kind(), cell)));
    }

    public LoginResult login(String username, String password) {
        CompletableFuture<WireMessage> reply = enqueuePendingReply();
        send(Protocol.encode(new LoginCommand(username, password)));
        return awaitLoginReply(reply);
    }

    public void updateSelection(Position selected) {
        // The server replies OK to a SELECT too; enqueue a (never-awaited) placeholder so that reply is
        // consumed here rather than being mistaken for a later request's reply (same reasoning as
        // requestJump's placeholder below).
        enqueuePendingReply();
        send(Protocol.encode(new SelectCommand(selected)));
    }

    public void newGame() {
        enqueuePendingReply();
        send(Protocol.encode(new NewGameCommand()));
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
            return new LoginResult(false, null, 0, "timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new LoginResult(false, null, 0, "interrupted");
        } catch (ExecutionException e) {
            return new LoginResult(false, null, 0, "error");
        }
    }

    private LoginResult toLoginResult(WireMessage message) {
        return switch (message) {
            case Welcome w -> new LoginResult(true, w.color(), w.rating(), "ok");
            case MoveRejected r -> new LoginResult(false, null, 0, r.reason());
            default -> new LoginResult(false, null, 0, "unexpected_message");
        };
    }
}
