package engine;
import model.*;
import config.GameConfig;
public class GameEngine {
    private final IBoard board;
    private final GameState gameState;
    private final GameClock gameClock;
    private final MoveValidator moveValidator;

    public GameEngine(IBoard board) {
        this.board = board;
        this.gameState = new GameState();
        this.gameClock = new GameClock();
        this.moveValidator = new MoveValidator(board);
    }

    public void handleClick(int pixelX, int pixelY) {
        if (gameState.isGameOver()) return;

        updateBoardState();

        int col = pixelX / GameConfig.PIXELS_PER_CELL;
        int row = pixelY / GameConfig.PIXELS_PER_CELL;

        if (!board.isWithinBounds(row, col)) return;

        String targetCell = board.getPieceAt(row, col);

        if (gameState.hasSelection()) {
            processMoveAttempt(row, col, targetCell);
        } else {
            processSelection(row, col, targetCell);
        }
    }

    public void handleJump(int pixelX, int pixelY) {
        if (gameState.isGameOver()) return;

        updateBoardState();

        int col = pixelX / GameConfig.PIXELS_PER_CELL;
        int row = pixelY / GameConfig.PIXELS_PER_CELL;

        if (!board.isWithinBounds(row, col)) return;

        String targetCell = board.getPieceAt(row, col);

        if (!gameState.hasSelection()) return;

        int startRow = gameState.getSelectedRow();
        int startCol = gameState.getSelectedCol();

        if (startRow == row && startCol == col) {
            gameState.clearSelection();
            return;
        }

        String movingPieceStr = board.getPieceAt(startRow, startCol);
        if (movingPieceStr.equals(Piece.EMPTY) || gameState.isPieceInFlight(startRow, startCol)) {
            gameState.clearSelection();
            return;
        }

        Piece movingPiece = Piece.fromString(movingPieceStr);

        if (moveValidator.isValidMove(movingPiece.getType(), movingPiece.getColor(), startRow, startCol, row, col)) {
            if (gameState.isTargetClaimed(row, col) || gameState.isPieceAirborne(row, col, gameClock.getCurrentTime())) {
                gameState.clearSelection();
                return;
            }

            if (!targetCell.equals(Piece.EMPTY) && Piece.fromString(targetCell).isSameColorAs(movingPiece)) {
                gameState.clearSelection();
                return;
            }

            long startTime = gameClock.getCurrentTime();
            long endTime = startTime + GameConfig.JUMP_DURATION_MS;
            JumpState jump = new JumpState(startRow, startCol, startTime, endTime);
            gameState.addJump(jump);

            long arrivalTime = gameClock.getCurrentTime() + GameConfig.MOVE_TRAVEL_TIME_MS;
            PendingMove move = new PendingMove(startRow, startCol, row, col, movingPieceStr, arrivalTime);
            gameState.addPendingMove(move);
        }

        gameState.clearSelection();
    }

    public void advanceTime(long ms) {
        if (gameState.isGameOver()) return;
        gameClock.advance(ms);
        updateBoardState();
    }

    public void printBoard() {
        updateBoardState();
        board.printBoard();
    }

    private void updateBoardState() {
        long currentTime = gameClock.getCurrentTime();
        gameState.getPendingMoves().stream()
                .filter(move -> move.hasArrived(currentTime))
                .forEach(this::executeMove);
        gameState.cleanupCompletedJumps(currentTime);
    }

    private void processSelection(int row, int col, String targetCell) {
        if (!targetCell.equals(Piece.EMPTY) && !gameState.isPieceInFlight(row, col)) {
            gameState.selectPiece(row, col);
        }
    }

    private void processMoveAttempt(int row, int col, String targetCell) {
        int startRow = gameState.getSelectedRow();
        int startCol = gameState.getSelectedCol();

        if (startRow == row && startCol == col) {
            gameState.clearSelection();
            return;
        }

        String movingPieceStr = board.getPieceAt(startRow, startCol);
        if (movingPieceStr.equals(Piece.EMPTY) || gameState.isPieceInFlight(startRow, startCol)) {
            gameState.clearSelection();
            return;
        }

        Piece movingPiece = Piece.fromString(movingPieceStr);

        if (moveValidator.isValidMove(movingPiece.getType(), movingPiece.getColor(), startRow, startCol, row, col)) {
            if (gameState.isTargetClaimed(row, col) || gameState.isPieceAirborne(row, col, gameClock.getCurrentTime())) {
                gameState.clearSelection();
                return;
            }

            if (!targetCell.equals(Piece.EMPTY) && Piece.fromString(targetCell).isSameColorAs(movingPiece)) {
                gameState.clearSelection();
                return;
            }

            long arrivalTime = gameClock.getCurrentTime() + GameConfig.MOVE_TRAVEL_TIME_MS;
            PendingMove move = new PendingMove(startRow, startCol, row, col, movingPieceStr, arrivalTime);
            gameState.addPendingMove(move);
        }

        gameState.clearSelection();
    }

    private void executeMove(PendingMove move) {
        gameState.removePendingMove(move);

        if (board.getPieceAt(move.getStartRow(), move.getStartCol()).equals(Piece.EMPTY)) {
            return;
        }

        JumpState interceptingJump = gameState.findActiveJumpAt(move.getStartRow(), move.getStartCol(), move.getArrivalTime());
        if (interceptingJump != null) {
            if (Piece.fromString(move.getPiece()).isKing()) {
                gameState.endGame();
            }
            board.clearCell(move.getStartRow(), move.getStartCol());
            return;
        }

        String capturedPiece = board.getPieceAt(move.getTargetRow(), move.getTargetCol());
        if (!capturedPiece.equals(Piece.EMPTY) && Piece.fromString(capturedPiece).isKing()) {
            gameState.endGame();
        }

        String finalPiece = handlePawnPromotion(move);
        board.setPieceAt(move.getTargetRow(), move.getTargetCol(), finalPiece);
        board.clearCell(move.getStartRow(), move.getStartCol());
    }

    private String handlePawnPromotion(PendingMove move) {
        String piece = move.getPiece();
        char color = piece.charAt(0);
        char type = piece.charAt(1);

        if (type == Piece.PAWN) {
            boolean reachedLastRow = (color == Piece.WHITE && move.getTargetRow() == 0) ||
                                    (color == Piece.BLACK && move.getTargetRow() == board.getNumRows() - 1);
            if (reachedLastRow) {
                return String.valueOf(color) + Piece.QUEEN;
            }
        }

        return piece;
    }

    public IBoard getBoard() {
        return board;
    }

    public GameState getGameState() {
        return gameState;
    }

    public GameClock getGameClock() {
        return gameClock;
    }
}