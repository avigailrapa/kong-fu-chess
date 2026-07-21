package src.server;

import lombok.Getter;
import lombok.experimental.Accessors;
import src.engine.GameEngine;
import src.engine.MoveLogger;
import src.model.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Accessors(fluent = true)
public class Match {

    @Getter
    private GameEngine engine;
    @Getter
    private MoveLogger moveLogger;
    private final long tickIntervalMs;
    private final ScheduledExecutorService executor;
    private final List<Session> seated = new ArrayList<>();
    private final List<Session> spectators = new ArrayList<>();
    private Runnable onTick;
    private Runnable onNewGame = () -> {
    };
    private ScheduledFuture<?> tickTask;

    public Match(GameEngine engine, long tickIntervalMs) {
        this.engine = engine;
        this.tickIntervalMs = tickIntervalMs;
        this.moveLogger = new MoveLogger();
        engine.addMoveObserver(moveLogger);
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void onNewGame(Runnable listener) {
        this.onNewGame = listener;
    }

    public void newGame(GameEngine engine) {
        this.engine = engine;
        this.moveLogger = new MoveLogger();
        engine.addMoveObserver(moveLogger);
        onNewGame.run();
    }

    public void start(Runnable onTick) {
        if (tickTask != null) {
            throw new IllegalStateException("Match already started");
        }
        this.onTick = onTick;
        tickTask = executor.scheduleAtFixedRate(this::tick, tickIntervalMs, tickIntervalMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel(false);
        }
        executor.shutdown();
    }

    public void submit(Runnable task) {
        executor.execute(task);
    }

    public Optional<Piece.Color> assignSeat() {
        if (seated.size() >= 2) {
            return Optional.empty();
        }
        boolean whiteTaken = seated.stream().anyMatch(s -> s.assignedColor() == Piece.Color.WHITE);
        return Optional.of(whiteTaken ? Piece.Color.BLACK : Piece.Color.WHITE);
    }

    public void addSession(Session session) {
        seated.add(session);
    }

    public List<Session> seated() {
        return List.copyOf(seated);
    }

    public void addSpectator(Session session) {
        spectators.add(session);
    }

    public List<Session> spectators() {
        return List.copyOf(spectators);
    }

    private void tick() {
        engine.waitMs(tickIntervalMs);
        onTick.run();
    }
}
