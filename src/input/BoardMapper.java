package input;

import model.Position;

import java.util.Optional;

public class BoardMapper {

    private static final int CELL_SIZE = 100;

    private final int width;
    private final int height;

    public BoardMapper(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Optional<Position> pixelToCell(int x, int y) {
        int col = Math.floorDiv(x, CELL_SIZE);
        int row = Math.floorDiv(y, CELL_SIZE);

        if (row < 0 || row >= height || col < 0 || col >= width) {
            return Optional.empty();
        }
        return Optional.of(new Position(row, col));
    }
}
