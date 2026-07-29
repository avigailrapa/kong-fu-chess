package src.server.core;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import src.engine.GameEngine;
import src.engine.MoveLogger;
import src.model.Piece;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
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
    @Getter
    @Setter
    private String matchId;
    private final long tickIntervalMs;
    private final ScheduledExecutorService executor;
    private final List<Session> seated = new CopyOnWriteArrayList<>();
    private final List<Session> spectators = new CopyOnWriteArrayList<>();
    private Runnable onTick;
    private final List<Runnable> onNewGameListeners = new CopyOnWriteArrayList<>();
    private ScheduledFuture<?> tickTask;

    public Match(GameEngine engine, long tickIntervalMs) {
        this.tickIntervalMs = tickIntervalMs;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        rewire(engine);
    }

    public void onNewGame(Runnable listener) {
        onNewGameListeners.add(listener);
    }

    public void newGame(GameEngine engine) {
        rewire(engine);
        onNewGameListeners.forEach(Runnable::run);
    }

    private void rewire(GameEngine engine) {
        this.engine = engine;
        this.moveLogger = new MoveLogger();
        engine.addMoveObserver(moveLogger);
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
