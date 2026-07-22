package src.net.client;

public record LoginResult(boolean accepted, int rating, String reason) {
}
