package src.realtime;

import src.model.Piece;
import src.model.Position;

public record Motion(Piece piece, Position source, Position destination, long durationMs) {

    public Position cellBeforeDestination() {
        int deltaRow = destination.row() - source.row();
        int deltaCol = destination.col() - source.col();
        boolean isStraightLine = deltaRow == 0 || deltaCol == 0 || Math.abs(deltaRow) == Math.abs(deltaCol);
        if (!isStraightLine) {
            return source;
        }

        int rowStep = Integer.signum(deltaRow);
        int colStep = Integer.signum(deltaCol);
        return new Position(destination.row() - rowStep, destination.col() - colStep);
    }
}
