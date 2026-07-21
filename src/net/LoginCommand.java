package src.net;

public record LoginCommand(String username, String password) implements WireMessage {
}
