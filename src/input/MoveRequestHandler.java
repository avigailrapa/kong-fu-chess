package input;

import model.Position;

public interface MoveRequestHandler {
    MoveResult requestMove(Position source, Position destination);
}
