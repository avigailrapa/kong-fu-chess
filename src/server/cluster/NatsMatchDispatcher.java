package src.server.cluster;

import io.nats.client.Connection;
import src.server.coordinator.Coordinator;

import java.util.List;

public class NatsMatchDispatcher implements Coordinator.MatchDispatcher {

    private final Connection nats;

    public NatsMatchDispatcher(Connection nats) {
        this.nats = nats;
    }

    @Override
    public void createMatch(String nodeId, String matchId, List<Coordinator.PlayerAssignment> players) {
        List<ClusterProtocol.PlayerAssignment> wirePlayers = players.stream()
                .map(p -> new ClusterProtocol.PlayerAssignment(p.connectionId(), p.username(), p.rating(),
                        p.color().name()))
                .toList();
        nats.publish(ClusterProtocol.createMatchSubject(nodeId),
                ClusterProtocol.encodeCreateMatch(new ClusterProtocol.CreateMatchCommand(matchId, wirePlayers)));
    }

    @Override
    public void reconnect(String nodeId, String username, String connectionId) {
        nats.publish(ClusterProtocol.reconnectSubject(nodeId),
                ClusterProtocol.encodeReconnect(new ClusterProtocol.ReconnectCommand(username, connectionId)));
    }
}
