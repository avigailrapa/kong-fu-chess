package src.net.messages;

import src.model.Position;

public record SelectCommand(Position selected) implements WireMessage {
}
