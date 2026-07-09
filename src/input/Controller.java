package input;

import model.IBoard;
import model.Position;

import java.util.Optional;

public class Controller {

    private final BoardMapper boardMapper;
    private final IBoard board;
    private final MoveRequestHandler moveRequestHandler;

    private Position selectedCell;

    public Controller(BoardMapper boardMapper, IBoard board, MoveRequestHandler moveRequestHandler) {
        this.boardMapper = boardMapper;
        this.board = board;
        this.moveRequestHandler = moveRequestHandler;
        this.selectedCell = null;
    }

    public void click(int x, int y) {
        Optional<Position> cell = boardMapper.pixelToCell(x, y);

        if (cell.isEmpty()) {
            selectedCell = null;
            return;
        }

        if (selectedCell == null) {
            if (board.getPieceAt(cell.get()).isPresent()) {
                selectedCell = cell.get();
            }
            return;
        }

        moveRequestHandler.requestMove(selectedCell, cell.get());
        selectedCell = null;
    }

    public Optional<Position> getSelectedCell() {
        return Optional.ofNullable(selectedCell);
    }
}
