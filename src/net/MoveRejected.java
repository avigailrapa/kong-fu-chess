package src.net;

public record MoveRejected(String reason) implements WireMessage {
}
