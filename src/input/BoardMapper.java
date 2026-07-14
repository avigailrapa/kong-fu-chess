package src.input;

import src.model.Position;

import java.util.Optional;

import src.view.GameSnapshot;

public class BoardMapper {

    private final int width;
    private final int height;

    public BoardMapper(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Optional<Position> pixelToCell(int x, int y) {
        int col = (int) Math.floor(x / GameSnapshot.CELL_WIDTH);
        int row = (int) Math.floor(y / GameSnapshot.CELL_HEIGHT);

        if (row < 0 || row >= height || col < 0 || col >= width) {
            return Optional.empty();
        }
        return Optional.of(new Position(row, col));
    }
}
