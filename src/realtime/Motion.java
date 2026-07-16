package src.realtime;

import src.model.Piece;
import src.model.Position;

public record Motion(Piece piece, Position source, Position destination, long durationMs) {

    /**
     * The straight-line cell immediately before the destination, for a sliding piece bouncing
     * off a friendly occupant. Falls back to the source itself when the move isn't a straight
     * line (e.g. a knight) or is only one square long, since there is no cell in between.
     */
    public Position cellBeforeDestination() {
        int deltaRow = destination.getRow() - source.getRow();
        int deltaCol = destination.getCol() - source.getCol();
        boolean isStraightLine = deltaRow == 0 || deltaCol == 0 || Math.abs(deltaRow) == Math.abs(deltaCol);
        if (!isStraightLine) {
            return source;
        }

        int rowStep = Integer.signum(deltaRow);
        int colStep = Integer.signum(deltaCol);
        return new Position(destination.getRow() - rowStep, destination.getCol() - colStep);
    }
}
