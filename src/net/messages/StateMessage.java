package src.net.messages;

import src.view.snapshot.GameSnapshot;

public record StateMessage(GameSnapshot snapshot) implements WireMessage {
}
