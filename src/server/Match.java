package src.server;

import src.engine.GameEngine;
import src.engine.MoveLogger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Match {

    private final GameEngine engine;
    private final MoveLogger moveLogger;
    private final long tickIntervalMs;
    private final ScheduledExecutorService executor;
    private Runnable onTick;
    private ScheduledFuture<?> tickTask;

    public Match(GameEngine engine, long tickIntervalMs) {
        this.engine = engine;
        this.tickIntervalMs = tickIntervalMs;
        this.moveLogger = new MoveLogger();
        engine.addMoveObserver(moveLogger);
        this.executor = Executors.newSingleThreadScheduledExecutor();
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

    public GameEngine engine() {
        return engine;
    }

    public MoveLogger moveLogger() {
        return moveLogger;
    }

    private void tick() {
        engine.waitMs(tickIntervalMs);
        onTick.run();
    }
}
