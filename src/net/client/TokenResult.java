package src.net.client;

public record TokenResult(boolean accepted, int rating, String token, String reason) {
}
