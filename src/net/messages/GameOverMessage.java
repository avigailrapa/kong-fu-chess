package src.net.messages;

import src.engine.GameOverEvent;

public record GameOverMessage(GameOverEvent event) implements WireMessage {
}
