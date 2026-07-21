package src.net;

public record RoomJoinCommand(String roomId) implements WireMessage {
}
