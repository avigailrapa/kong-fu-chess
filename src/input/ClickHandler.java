package src.input;

import lombok.RequiredArgsConstructor;
import src.engine.GameCommands;
import src.engine.MoveResult;
import src.model.Position;

import java.util.Optional;

@RequiredArgsConstructor
public class ClickHandler {

    private final BoardMapper boardMapper;
    private final GameCommands gameEngine;
    private Position selectedCell;

    public void setZoom(double zoom) {
        boardMapper.setZoom(zoom);
    }

    public Optional<MoveResult> click(int x, int y) {
        Optional<Position> clicked = boardMapper.pixelToCell(x, y);

        if (clicked.isEmpty()) {
            selectedCell = null;
            return Optional.empty();
        }

        Position cell = clicked.get();

        if (selectedCell == null) {
            if (gameEngine.isOccupied(cell)) {
                selectedCell = cell;
            }
            return Optional.empty();
        }

        MoveResult result = gameEngine.requestMove(selectedCell, cell);
        if (!result.isAccepted() && "friendly_destination".equals(result.reason())) {
            selectedCell = cell;
            return Optional.empty();
        }
        selectedCell = null;
        return Optional.of(result);
    }

    public void jump(int x, int y) {
        boardMapper.pixelToCell(x, y).ifPresent(gameEngine::requestJump);
    }

    public Optional<Position> getSelectedCell() {
        return Optional.ofNullable(selectedCell);
    }
}
