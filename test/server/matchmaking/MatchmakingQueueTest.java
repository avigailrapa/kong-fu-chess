package server.matchmaking;

import org.junit.jupiter.api.Test;
import src.server.matchmaking.MatchmakingQueue;
import src.server.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class MatchmakingQueueTest {

    private Session session(String username, int rating) {
        return new Session(text -> {
        }, username, rating);
    }

    @Test
    public void testFirstEnqueuedSessionWaitsWithoutPairing() {
        List<Session[]> paired = new ArrayList<>();
        MatchmakingQueue queue = new MatchmakingQueue(
                (a, b) -> paired.add(new Session[]{a, b}), s -> {
        }, 5000, 100);

        queue.enqueue(session("alice", 1200));

        assertTrue(paired.isEmpty());
    }

    @Test
    public void testSecondSessionWithinRatingWindowIsPaired() {
        List<Session[]> paired = new ArrayList<>();
        MatchmakingQueue queue = new MatchmakingQueue(
                (a, b) -> paired.add(new Session[]{a, b}), s -> {
        }, 5000, 100);
        Session alice = session("alice", 1200);
        Session bob = session("bob", 1250);

        queue.enqueue(alice);
        queue.enqueue(bob);

        assertEquals(1, paired.size());
        assertArrayEquals(new Session[]{alice, bob}, paired.get(0));
    }

    @Test
    public void testSessionOutsideRatingWindowIsNotPaired() {
        List<Session[]> paired = new ArrayList<>();
        MatchmakingQueue queue = new MatchmakingQueue(
                (a, b) -> paired.add(new Session[]{a, b}), s -> {
        }, 5000, 100);

        queue.enqueue(session("alice", 1200));
        queue.enqueue(session("bob", 1400));

        assertTrue(paired.isEmpty());
    }

    @Test
    public void testLoneWaitingSessionTimesOut() throws InterruptedException {
        CountDownLatch timedOut = new CountDownLatch(1);
        MatchmakingQueue queue = new MatchmakingQueue((a, b) -> {
        }, s -> timedOut.countDown(), 50, 100);

        queue.enqueue(session("alice", 1200));

        assertTrue(timedOut.await(2, TimeUnit.SECONDS), "expected the lone session to time out");
    }

    @Test
    public void testCancelPreventsLaterTimeout() throws InterruptedException {
        CountDownLatch timedOut = new CountDownLatch(1);
        MatchmakingQueue queue = new MatchmakingQueue((a, b) -> {
        }, s -> timedOut.countDown(), 50, 100);
        Session alice = session("alice", 1200);

        queue.enqueue(alice);
        queue.cancel(alice);

        assertFalse(timedOut.await(200, TimeUnit.MILLISECONDS), "cancelled session should not time out");
    }
}
