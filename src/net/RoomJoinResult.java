package src.net;

public record RoomJoinResult(boolean accepted, boolean spectating, String reason) {
}
