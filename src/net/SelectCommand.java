package src.net;

import src.model.Position;

public record SelectCommand(Position selected) implements WireMessage {
}
