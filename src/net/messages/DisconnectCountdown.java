package src.net.messages;

public record DisconnectCountdown(int secondsRemaining) implements WireMessage {
}
