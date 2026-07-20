package src.engine;

import src.model.Position;

public interface GameCommands {
    boolean isOccupied(Position position);

    MoveResult requestMove(Position source, Position destination);

    void requestJump(Position cell);
}
