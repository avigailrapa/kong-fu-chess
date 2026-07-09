package input;

import model.Position;

import java.util.ArrayList;
import java.util.List;

class FakeMoveRequestHandler implements MoveRequestHandler {

    private final List<Position> sources = new ArrayList<>();
    private final List<Position> destinations = new ArrayList<>();

    @Override
    public MoveResult requestMove(Position source, Position destination) {
        sources.add(source);
        destinations.add(destination);
        return new MoveResult(true, "ok");
    }

    int callCount() {
        return sources.size();
    }

    Position lastSource() {
        return sources.get(sources.size() - 1);
    }

    Position lastDestination() {
        return destinations.get(destinations.size() - 1);
    }
}
