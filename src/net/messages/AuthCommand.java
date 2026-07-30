package src.net.messages;

public record AuthCommand(String token) implements WireMessage {
}
