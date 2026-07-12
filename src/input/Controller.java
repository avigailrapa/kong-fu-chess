package input;

import engine.GameEngine;
import model.Position;

import java.util.Optional;

public class Controller {

    private final BoardMapper boardMapper;
    private final GameEngine gameEngine;
    private Position selectedCell;

    public Controller(BoardMapper boardMapper, GameEngine gameEngine) {
        this.boardMapper = boardMapper;
        this.gameEngine = gameEngine;
    }

    public void click(int x, int y) {
        Optional<Position> clicked = boardMapper.pixelToCell(x, y);

        if (clicked.isEmpty()) {
            selectedCell = null;
            return;
        }

        Position cell = clicked.get();

        if (selectedCell == null) {
            if (gameEngine.snapshot().isOccupied(cell)) {
                selectedCell = cell;
            }
            return;
        }

        gameEngine.requestMove(selectedCell, cell);
        selectedCell = null;
    }

    public Optional<Position> getSelectedCell() {
        return Optional.ofNullable(selectedCell);
    }
}
