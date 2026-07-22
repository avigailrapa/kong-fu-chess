package src.net.messages;

import src.engine.MoveEvent;

public record MoveOccurred(MoveEvent event) implements WireMessage {
}
