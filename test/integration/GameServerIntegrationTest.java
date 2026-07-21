package integration;

import org.junit.jupiter.api.Test;
import src.server.ActivityLog;
import src.server.GameServer;
import src.server.UserStore;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class GameServerIntegrationTest {

    @Test
    public void testRealSocketRoundTripAcceptsAMoveAndBroadcastsState() throws Exception {
        ActivityLog activityLog = new ActivityLog(File.createTempFile("kongfu-activity", ".log").getAbsolutePath());
        GameServer server = new GameServer(new InetSocketAddress("localhost", 0),
                new UserStore("jdbc:sqlite::memory:"), 100, 20, activityLog);
        server.start();

        WebSocket clientA = null;
        WebSocket clientB = null;
        try {
            int port = waitForBoundPort(server);
            AtomicReference<String> matchFoundA = new AtomicReference<>();
            AtomicReference<String> matchFoundB = new AtomicReference<>();
            AtomicInteger stateCountA = new AtomicInteger();
            CountDownLatch gotOk = new CountDownLatch(1);

            clientA = connect(port, matchFoundA, stateCountA, gotOk);
            clientB = connect(port, matchFoundB, new AtomicInteger(), new CountDownLatch(1));

            clientA.sendText("LOGIN alice pw", true).get(5, TimeUnit.SECONDS);
            clientB.sendText("LOGIN bob pw", true).get(5, TimeUnit.SECONDS);
            clientA.sendText("PLAY", true).get(5, TimeUnit.SECONDS);
            clientB.sendText("PLAY", true).get(5, TimeUnit.SECONDS);

            waitUntil(() -> matchFoundA.get() != null && matchFoundB.get() != null,
                    "both clients to receive MATCH_FOUND");
            int stateCountBeforeMove = stateCountA.get();
            waitUntil(() -> stateCountA.get() > 0, "a STATE broadcast after pairing");
            stateCountBeforeMove = stateCountA.get();

            String colorA = matchFoundA.get().split(" ")[2];
            WebSocket whiteClient = colorA.equals("W") ? clientA : clientB;

            whiteClient.sendText("WNb1a3", true).get(5, TimeUnit.SECONDS);

            assertTrue(gotOk.await(5, TimeUnit.SECONDS), "expected an OK reply over the real socket");
            int finalStateCountBeforeMove = stateCountBeforeMove;
            waitUntil(() -> stateCountA.get() > finalStateCountBeforeMove,
                    "a STATE broadcast triggered by the move");
        } finally {
            if (clientA != null) {
                clientA.abort();
            }
            if (clientB != null) {
                clientB.abort();
            }
            server.stop();
        }
    }

    private WebSocket connect(int port, AtomicReference<String> matchFound, AtomicInteger stateCount,
                               CountDownLatch gotOk) throws Exception {
        WebSocket.Listener listener = new WebSocket.Listener() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                buffer.append(data);
                if (last) {
                    String message = buffer.toString();
                    buffer.setLength(0);
                    if (message.equals("OK")) {
                        gotOk.countDown();
                    } else if (message.startsWith("MATCH_FOUND ")) {
                        matchFound.set(message);
                    } else if (message.startsWith("STATE ")) {
                        stateCount.incrementAndGet();
                    }
                }
                webSocket.request(1);
                return null;
            }
        };
        return HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:" + port), listener)
                .get(5, TimeUnit.SECONDS);
    }

    private void waitUntil(java.util.function.BooleanSupplier condition, String descriptionOfWhatWeWaitedFor)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("timed out waiting for " + descriptionOfWhatWeWaitedFor);
    }

    private int waitForBoundPort(GameServer server) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            int port = server.getPort();
            if (port > 0) {
                return port;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("server did not bind a port in time");
    }
}
