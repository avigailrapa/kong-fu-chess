package src.net;

public record LoginCommand(String username) implements WireMessage {
}
