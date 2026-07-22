package src.net.messages;

public record RoomJoinCommand(String roomId) implements WireMessage {
}
