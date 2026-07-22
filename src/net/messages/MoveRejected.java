package src.net.messages;

public record MoveRejected(String reason) implements WireMessage {
}
