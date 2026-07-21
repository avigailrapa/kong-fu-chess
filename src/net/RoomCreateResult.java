package src.net;

public record RoomCreateResult(boolean accepted, String roomId, String reason) {
}
