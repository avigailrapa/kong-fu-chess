package src.engine;

import src.model.Position;

import java.util.Set;

public class GameSnapshot {
    private final Set<Position> occupiedCells;

    public GameSnapshot(Set<Position> occupiedCells) {
        this.occupiedCells = occupiedCells;
    }

    public Set<Position> occupiedCells() {
        return occupiedCells;
    }

    public boolean isOccupied(Position position) {
        return occupiedCells.contains(position);
    }
}