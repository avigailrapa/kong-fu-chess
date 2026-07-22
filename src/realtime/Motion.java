package src.realtime;

import src.model.Piece;
import src.model.Position;

import java.util.ArrayList;
import java.util.List;

public record Motion(Piece piece, Position source, Position destination, long durationMs) {

    public boolean isStraightLine() {
        int deltaRow = destination.row() - source.row();
        int deltaCol = destination.col() - source.col();
        return deltaRow == 0 || deltaCol == 0 || Math.abs(deltaRow) == Math.abs(deltaCol);
    }

    public Position cellBeforeDestination() {
        if (!isStraightLine()) {
            return source;
        }
        return new Position(destination.row() - rowStep(), destination.col() - colStep());
    }

    public Position cellAtDistance(int distance) {
        return new Position(source.row() + rowStep() * distance, source.col() + colStep() * distance);
    }

    public List<Position> intermediateCells() {
        List<Position> cells = new ArrayList<>();
        if (!isStraightLine()) {
            return cells;
        }

        Position current = source;
        while (!current.equals(destination)) {
            current = new Position(current.row() + rowStep(), current.col() + colStep());
            if (current.equals(destination)) {
                break;
            }
            cells.add(current);
        }
        return cells;
    }

    private int rowStep() {
        return Integer.signum(destination.row() - source.row());
    }

    private int colStep() {
        return Integer.signum(destination.col() - source.col());
    }
}
