package src.net.client;

public record RoomCreateResult(boolean accepted, String roomId, String reason) {
}
