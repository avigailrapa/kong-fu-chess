package src.net;

public record DisconnectCountdown(int secondsRemaining) implements WireMessage {
}
