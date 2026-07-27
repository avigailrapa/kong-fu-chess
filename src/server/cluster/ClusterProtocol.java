package src.server.cluster;

import java.nio.charset.StandardCharsets;

public class ClusterProtocol {

    public static final String INBOUND_SUBJECT = "ws.in";
    public static final String DISCONNECT_SUBJECT = "ws.disconnect";
    private static final String OUTBOUND_SUBJECT_PREFIX = "ws.out.";
    public static final String OUTBOUND_WILDCARD_SUBJECT = OUTBOUND_SUBJECT_PREFIX + ">";
    private static final char DELIMITER = (char) 1;

    public record Envelope(String connectionId, String message) {
    }

    public static byte[] encodeInbound(String connectionId, String message) {
        return (connectionId + DELIMITER + message).getBytes(StandardCharsets.UTF_8);
    }

    public static Envelope decodeInbound(byte[] payload) {
        String text = new String(payload, StandardCharsets.UTF_8);
        int index = text.indexOf(DELIMITER);
        return new Envelope(text.substring(0, index), text.substring(index + 1));
    }

    public static String outboundSubject(String connectionId) {
        return OUTBOUND_SUBJECT_PREFIX + connectionId;
    }

    public static String connectionIdFromOutboundSubject(String subject) {
        return subject.substring(OUTBOUND_SUBJECT_PREFIX.length());
    }
}
