package src.server;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

@RequiredArgsConstructor
public class ReconnectManager {

    public record Pending(Session session, Match match) {
    }

    private record Entry(Session session, Match match, List<ScheduledFuture<?>> countdownTasks) {
    }

    private final int disconnectCountdownSeconds;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Entry> pending = new ConcurrentHashMap<>();

    public void startCountdown(Match match, Session disconnected, IntConsumer onTick, Runnable onExpire) {
        List<ScheduledFuture<?>> countdownTasks = new ArrayList<>();
        for (int elapsed = 1; elapsed <= disconnectCountdownSeconds; elapsed++) {
            int secondsRemaining = disconnectCountdownSeconds - elapsed;
            countdownTasks.add(executor.schedule(() -> {
                if (secondsRemaining > 0) {
                    onTick.accept(secondsRemaining);
                } else {
                    pending.remove(disconnected.username());
                    onExpire.run();
                }
            }, elapsed, TimeUnit.SECONDS));
        }
        pending.put(disconnected.username(), new Entry(disconnected, match, countdownTasks));
    }

    public Optional<Pending> pendingFor(String username) {
        Entry entry = pending.get(username);
        return entry == null ? Optional.empty() : Optional.of(new Pending(entry.session(), entry.match()));
    }

    public void cancelCountdown(String username) {
        Entry entry = pending.remove(username);
        if (entry != null) {
            entry.countdownTasks().forEach(task -> task.cancel(false));
        }
    }
}
