package src.net.messages;

public record LoginCommand(String username, String password) implements WireMessage {
}
