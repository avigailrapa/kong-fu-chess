package src.bus;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EventBus {

    private final Map<Class<?>, List<Consumer<Object>>> subscribers = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<? super T> handler) {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        subscribers.computeIfAbsent(eventType, unused -> new CopyOnWriteArrayList<>())
                .add((Consumer<Object>) handler);
    }

    public void publish(Object event) {
        Objects.requireNonNull(event, "event must not be null");
        List<Consumer<Object>> handlers = subscribers.get(event.getClass());
        if (handlers == null) {
            return;
        }
        for (Consumer<Object> handler : handlers) {
            handler.accept(event);
        }
    }
}
