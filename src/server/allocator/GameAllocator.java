package src.server.allocator;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import src.server.cluster.ClusterProtocol;

import java.util.concurrent.atomic.AtomicLong;

public class GameAllocator {

    private static final Logger log = LoggerFactory.getLogger(GameAllocator.class);

    private final Allocator allocator;
    private final ConnectionDirectory directory;
    private final Connection nats;
    private final AtomicLong matchesAllocated = new AtomicLong();

    public GameAllocator(Allocator allocator, ConnectionDirectory directory, Connection nats) {
        this.allocator = allocator;
        this.directory = directory;
        this.nats = nats;
    }

    public long matchesAllocated() {
        return matchesAllocated.get();
    }

    public void start() {
        Dispatcher dispatcher = nats.createDispatcher(this::onAssign);
        dispatcher.subscribe(ClusterProtocol.ASSIGN_SUBJECT);
    }

    private void onAssign(io.nats.client.Message message) {
        ClusterProtocol.CreateMatchCommand command;
        try {
            command = ClusterProtocol.decodeCreateMatch(message.getData());
        } catch (RuntimeException e) {
            log.warn("malformed assign request, dropping", e);
            return;
        }
        String nodeId = allocator.pickNode();
        for (ClusterProtocol.PlayerAssignment player : command.players()) {
            directory.assignConnection(player.connectionId(), nodeId);
            directory.assignUser(player.username(), nodeId);
        }
        log.info("match {} - allocated to {}", command.matchId(), nodeId);
        nats.publish(ClusterProtocol.createMatchSubject(nodeId), message.getData());
        matchesAllocated.incrementAndGet();
    }
}
