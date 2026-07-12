package engine;

import model.Position;

import java.util.Set;

public record GameSnapshot(Set<Position> occupiedCells) {

    public boolean isOccupied(Position position) {
        return occupiedCells.contains(position);
    }
}
