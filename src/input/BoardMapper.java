package src.input;

import lombok.RequiredArgsConstructor;
import src.model.Position;

import java.util.Optional;

import src.view.GameSnapshot;

@RequiredArgsConstructor
public class BoardMapper {

    private final int width;
    private final int height;
    private double zoom = GameSnapshot.DEFAULT_ZOOM;

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    public Optional<Position> pixelToCell(int x, int y) {
        int col = (int) Math.floor(x / (GameSnapshot.CELL_WIDTH * zoom));
        int row = (int) Math.floor(y / (GameSnapshot.CELL_HEIGHT * zoom));

        if (row < 0 || row >= height || col < 0 || col >= width) {
            return Optional.empty();
        }
        return Optional.of(new Position(row, col));
    }
}
