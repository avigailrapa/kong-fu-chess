package src.net;

import src.engine.MoveEvent;

public record MoveOccurred(MoveEvent event) implements WireMessage {
}
