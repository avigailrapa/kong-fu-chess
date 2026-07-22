package src.net.client;

public record RoomJoinResult(boolean accepted, boolean spectating, String reason) {
}
