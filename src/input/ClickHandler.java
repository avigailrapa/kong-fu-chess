package src.input;

import lombok.RequiredArgsConstructor;
import src.engine.GameCommands;
import src.engine.MoveResult;
import src.model.Position;

import java.util.Optional;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ClickHandler {

    private final BoardMapper boardMapper;
    private final GameCommands gameEngine;
    private Position selectedCell;

    public void setZoom(double zoom) {
        boardMapper.setZoom(zoom);
    }

    public void click(int x, int y) {
        click(x, y, result -> { });
    }

    public void click(int x, int y, Consumer<MoveResult> onResult) {
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

        gameEngine.requestMove(selectedCell, cell, result -> {
            if (!result.isAccepted() && "friendly_destination".equals(result.reason())) {
                selectedCell = cell;
            } else {
                selectedCell = null;
            }
            onResult.accept(result);
        });
    }

    public void jump(int x, int y) {
        boardMapper.pixelToCell(x, y).ifPresent(gameEngine::requestJump);
    }

    public Optional<Position> selectedCell() {
        return Optional.ofNullable(selectedCell);
    }
}
