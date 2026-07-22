package src.server;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class MatchmakingQueue {

    private final BiConsumer<Session, Session> onPaired;
    private final Consumer<Session> onTimeout;
    private final long timeoutMs;
    private final int ratingWindow;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final List<Session> waiting = new ArrayList<>();
    private final Map<Session, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();

    public synchronized void enqueue(Session session) {
        for (Session candidate : waiting) {
            if (Math.abs(candidate.rating() - session.rating()) <= ratingWindow) {
                waiting.remove(candidate);
                cancelTimeout(candidate);
                onPaired.accept(candidate, session);
                return;
            }
        }
        waiting.add(session);
        ScheduledFuture<?> task = executor.schedule(() -> handleTimeout(session), timeoutMs, TimeUnit.MILLISECONDS);
        timeoutTasks.put(session, task);
    }

    public synchronized void cancel(Session session) {
        if (waiting.remove(session)) {
            cancelTimeout(session);
        }
    }

    private synchronized void handleTimeout(Session session) {
        if (waiting.remove(session)) {
            timeoutTasks.remove(session);
            onTimeout.accept(session);
        }
    }

    private void cancelTimeout(Session session) {
        ScheduledFuture<?> task = timeoutTasks.remove(session);
        if (task != null) {
            task.cancel(false);
        }
    }
}
