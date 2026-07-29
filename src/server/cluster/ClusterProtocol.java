package src.server.cluster;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ClusterProtocol {

    public static final String INBOUND_SUBJECT = "ws.in";
    public static final String DISCONNECT_SUBJECT = "ws.disconnect";
    private static final String OUTBOUND_SUBJECT_PREFIX = "ws.out.";
    public static final String OUTBOUND_WILDCARD_SUBJECT = OUTBOUND_SUBJECT_PREFIX + ">";
    private static final char DELIMITER = (char) 1;

    public static final String LOBBY_INBOUND_SUBJECT = "ws.in.lobby";
    public static final String LOBBY_DISCONNECT_SUBJECT = "ws.disconnect.lobby";
    public static final String MATCH_ENDED_SUBJECT = "gamenode.match-ended";
    private static final String NODE_INBOUND_PREFIX = "ws.in.node.";
    private static final String NODE_DISCONNECT_PREFIX = "ws.disconnect.node.";
    private static final String CREATE_MATCH_PREFIX = "coordinator.create-match.";
    private static final String RECONNECT_PREFIX = "coordinator.reconnect.";

    private static final char UNIT_SEPARATOR = (char) 31;
    private static final char RECORD_SEPARATOR = (char) 30;

    public record Envelope(String connectionId, String message) {
    }

    public record PlayerAssignment(String connectionId, String username, int rating, String color) {
    }

    public record CreateMatchCommand(String matchId, List<PlayerAssignment> players) {
    }

    public record ReconnectCommand(String username, String connectionId) {
    }

    public record MatchEndedEvent(String matchId, List<String> usernames) {
    }

    public static byte[] encodeInbound(String connectionId, String message) {
        return (connectionId + DELIMITER + message).getBytes(StandardCharsets.UTF_8);
    }

    public static Envelope decodeInbound(byte[] payload) {
        String text = new String(payload, StandardCharsets.UTF_8);
        int index = text.indexOf(DELIMITER);
        if (index < 0) {
            throw new IllegalArgumentException("malformed inbound envelope: missing delimiter");
        }
        return new Envelope(text.substring(0, index), text.substring(index + 1));
    }

    public static String outboundSubject(String connectionId) {
        return OUTBOUND_SUBJECT_PREFIX + connectionId;
    }

    public static String connectionIdFromOutboundSubject(String subject) {
        return subject.substring(OUTBOUND_SUBJECT_PREFIX.length());
    }

    public static String nodeInboundSubject(String nodeId) {
        return NODE_INBOUND_PREFIX + nodeId;
    }

    public static String nodeDisconnectSubject(String nodeId) {
        return NODE_DISCONNECT_PREFIX + nodeId;
    }

    public static String createMatchSubject(String nodeId) {
        return CREATE_MATCH_PREFIX + nodeId;
    }

    public static String reconnectSubject(String nodeId) {
        return RECONNECT_PREFIX + nodeId;
    }

    public static byte[] encodeCreateMatch(CreateMatchCommand command) {
        StringBuilder sb = new StringBuilder(command.matchId());
        for (PlayerAssignment player : command.players()) {
            sb.append(RECORD_SEPARATOR)
                    .append(player.connectionId()).append(UNIT_SEPARATOR)
                    .append(player.username()).append(UNIT_SEPARATOR)
                    .append(player.rating()).append(UNIT_SEPARATOR)
                    .append(player.color());
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static CreateMatchCommand decodeCreateMatch(byte[] payload) {
        String[] records = new String(payload, StandardCharsets.UTF_8).split(String.valueOf(RECORD_SEPARATOR));
        String matchId = records[0];
        List<PlayerAssignment> players = new ArrayList<>();
        for (int i = 1; i < records.length; i++) {
            String[] fields = records[i].split(String.valueOf(UNIT_SEPARATOR));
            players.add(new PlayerAssignment(fields[0], fields[1], Integer.parseInt(fields[2]), fields[3]));
        }
        return new CreateMatchCommand(matchId, players);
    }

    public static byte[] encodeReconnect(ReconnectCommand command) {
        return (command.username() + UNIT_SEPARATOR + command.connectionId()).getBytes(StandardCharsets.UTF_8);
    }

    public static ReconnectCommand decodeReconnect(byte[] payload) {
        String[] fields = new String(payload, StandardCharsets.UTF_8).split(String.valueOf(UNIT_SEPARATOR));
        return new ReconnectCommand(fields[0], fields[1]);
    }

    public static byte[] encodeMatchEnded(MatchEndedEvent event) {
        return (event.matchId() + UNIT_SEPARATOR + String.join(String.valueOf(UNIT_SEPARATOR), event.usernames()))
                .getBytes(StandardCharsets.UTF_8);
    }

    public static MatchEndedEvent decodeMatchEnded(byte[] payload) {
        String[] fields = new String(payload, StandardCharsets.UTF_8).split(String.valueOf(UNIT_SEPARATOR));
        return new MatchEndedEvent(fields[0], List.of(fields).subList(1, fields.length));
    }
}
