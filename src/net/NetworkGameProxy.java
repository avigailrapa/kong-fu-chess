package src.net;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

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
    // being mistaken for the reply to whatever request we send next.
    private final LinkedBlockingQueue<CompletableFuture<MoveResult>> pendingReplies = new LinkedBlockingQueue<>();
    private volatile GameSnapshot latestSnapshot;

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
            case MoveAccepted _ -> completeOldestPendingReply(new MoveResult(true, "ok"));
            case MoveRejected r -> completeOldestPendingReply(new MoveResult(false, r.reason()));
            case MoveCommand _ -> {
            }
            case JumpCommand _ -> {
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
        CompletableFuture<MoveResult> reply = enqueuePendingReply();
        send(Protocol.encode(new MoveCommand(piece.color(), piece.kind(), source, destination)));
        return awaitReply(reply);
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

    public GameSnapshot latestSnapshot() {
        return latestSnapshot;
    }

    private PieceSnapshot pieceAt(Position position) {
        return latestSnapshot == null ? null : latestSnapshot.pieceAt(position);
    }

    private CompletableFuture<MoveResult> enqueuePendingReply() {
        CompletableFuture<MoveResult> reply = new CompletableFuture<>();
        pendingReplies.add(reply);
        return reply;
    }

    private void completeOldestPendingReply(MoveResult result) {
        CompletableFuture<MoveResult> reply = pendingReplies.poll();
        if (reply != null) {
            reply.complete(result);
        }
    }

    private MoveResult awaitReply(CompletableFuture<MoveResult> reply) {
        try {
            return reply.get(requestTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return new MoveResult(false, "timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new MoveResult(false, "interrupted");
        } catch (ExecutionException e) {
            return new MoveResult(false, "error");
        }
    }
}
