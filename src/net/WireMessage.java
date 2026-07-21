package src.net;

public sealed interface WireMessage
        permits MoveCommand, JumpCommand, MoveAccepted, MoveRejected, StateMessage, LoginCommand, Welcome,
        SelectCommand, MoveOccurred, GameOverMessage, NewGameCommand, RatingChanged, PlayCommand,
        CancelPlayCommand, MatchFound, MatchTimeout, DisconnectCountdown, RoomCreateCommand, RoomJoinCommand,
        RoomId, Spectating {
}
