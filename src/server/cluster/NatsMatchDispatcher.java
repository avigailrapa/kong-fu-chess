package src.server.cluster;

import io.nats.client.Connection;
import src.server.matchmaker.Matchmaker;

import java.util.List;

public class NatsMatchDispatcher implements Matchmaker.MatchDispatcher {

    private final Connection nats;

    public NatsMatchDispatcher(Connection nats) {
        this.nats = nats;
    }

    @Override
    public void assign(String matchId, List<Matchmaker.PlayerAssignment> players) {
        List<ClusterProtocol.PlayerAssignment> wirePlayers = players.stream()
                .map(p -> new ClusterProtocol.PlayerAssignment(p.connectionId(), p.username(), p.rating(),
                        p.color().name()))
                .toList();
        nats.publish(ClusterProtocol.ASSIGN_SUBJECT,
                ClusterProtocol.encodeCreateMatch(new ClusterProtocol.CreateMatchCommand(matchId, wirePlayers)));
    }

    @Override
    public void reconnect(String nodeId, String username, String connectionId) {
        nats.publish(ClusterProtocol.reconnectSubject(nodeId),
                ClusterProtocol.encodeReconnect(new ClusterProtocol.ReconnectCommand(username, connectionId)));
    }
}
