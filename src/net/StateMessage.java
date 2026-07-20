package src.net;

import src.view.GameSnapshot;

public record StateMessage(GameSnapshot snapshot) implements WireMessage {
}
