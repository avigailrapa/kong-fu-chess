package src.net;

import src.engine.GameOverEvent;

public record GameOverMessage(GameOverEvent event) implements WireMessage {
}
