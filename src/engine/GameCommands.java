package src.engine;

import src.model.Position;

import java.util.function.Consumer;

public interface GameCommands {
    boolean isOccupied(Position position);

    void requestMove(Position source, Position destination, Consumer<MoveResult> onResult);

    void requestJump(Position cell);
}
