package bus;

import src.bus.EventBus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class EventBusTest {

    private record EventA(String value) {
    }

    private record EventB(int value) {
    }

    @Test
    public void testSubscriberReceivesPublishedEvent() {
        EventBus bus = new EventBus();
        List<EventA> received = new ArrayList<>();
        bus.subscribe(EventA.class, received::add);

        bus.publish(new EventA("hello"));

        assertEquals(List.of(new EventA("hello")), received);
    }

    @Test
    public void testMultipleSubscribersAllReceiveEvent() {
        EventBus bus = new EventBus();
        List<EventA> firstReceived = new ArrayList<>();
        List<EventA> secondReceived = new ArrayList<>();
        bus.subscribe(EventA.class, firstReceived::add);
        bus.subscribe(EventA.class, secondReceived::add);

        bus.publish(new EventA("hello"));

        assertEquals(1, firstReceived.size());
        assertEquals(1, secondReceived.size());
    }

    @Test
    public void testSubscriberNotInvokedForDifferentEventType() {
        EventBus bus = new EventBus();
        List<EventA> received = new ArrayList<>();
        bus.subscribe(EventA.class, received::add);

        bus.publish(new EventB(42));

        assertTrue(received.isEmpty());
    }

    @Test
    public void testPublishWithNoSubscribersDoesNotThrow() {
        EventBus bus = new EventBus();
        assertDoesNotThrow(() -> bus.publish(new EventA("no one is listening")));
    }

    @Test
    public void testPublishNullThrowsNullPointerException() {
        EventBus bus = new EventBus();
        assertThrows(NullPointerException.class, () -> bus.publish(null));
    }

    @Test
    public void testSubscribeWithNullHandlerThrowsNullPointerException() {
        EventBus bus = new EventBus();
        assertThrows(NullPointerException.class, () -> bus.subscribe(EventA.class, null));
    }

    @Test
    public void testSubscribeWithNullEventTypeThrowsNullPointerException() {
        EventBus bus = new EventBus();
        assertThrows(NullPointerException.class, () -> bus.subscribe(null, event -> {
        }));
    }

    @Test
    public void testMultipleEventTypesTrackedIndependently() {
        EventBus bus = new EventBus();
        List<EventA> receivedA = new ArrayList<>();
        List<EventB> receivedB = new ArrayList<>();
        bus.subscribe(EventA.class, receivedA::add);
        bus.subscribe(EventB.class, receivedB::add);

        bus.publish(new EventB(7));

        assertTrue(receivedA.isEmpty());
        assertEquals(List.of(new EventB(7)), receivedB);
    }
}
