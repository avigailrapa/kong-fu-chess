package src.input;

import src.engine.GameEngine;
import src.engine.MoveResult;
import src.model.Position;

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
            if (gameEngine.isOccupied(cell)) {
                selectedCell = cell;
            }
            return;
        }

        MoveResult result = gameEngine.requestMove(selectedCell, cell);
        if (!result.isAccepted() && "friendly_destination".equals(result.reason())) {
            selectedCell = cell;
            return;
        }
        selectedCell = null;
    }

    public void jump(int x, int y) {
        boardMapper.pixelToCell(x, y).ifPresent(gameEngine::requestJump);
    }

    public Optional<Position> getSelectedCell() {
        return Optional.ofNullable(selectedCell);
    }
}
