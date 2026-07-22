package src.net.messages;

import src.view.GameSnapshot;

public record StateMessage(GameSnapshot snapshot) implements WireMessage {
}
