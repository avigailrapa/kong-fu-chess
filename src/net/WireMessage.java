package src.net;

public sealed interface WireMessage
        permits MoveCommand, JumpCommand, MoveAccepted, MoveRejected, StateMessage, LoginCommand, Welcome,
        SelectCommand, MoveOccurred, GameOverMessage {
}
