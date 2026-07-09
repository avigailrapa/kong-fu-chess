package io;
import model.*;
import config.*;
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

        String movingPieceStr = board.getPieceAt(row, col);
        if (movingPieceStr.equals(Piece.EMPTY)) {
            return;
        }

        boolean isAlreadyMoving = gameState.getPendingMoves().stream()
                .anyMatch(move -> move.getStartRow() == row && move.getStartCol() == col);
        if (isAlreadyMoving) {
            return;
        }

        long startTime = gameClock.getCurrentTime();
        long endTime = startTime + GameConfig.JUMP_DURATION_MS;
        JumpState jump = new JumpState(row, col, startTime, endTime);
        gameState.addJump(jump);
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
            if (gameState.isTargetClaimed(row, col)) {
                gameState.clearSelection();
                return;
            }

            if (!targetCell.equals(Piece.EMPTY) && Piece.fromString(targetCell).isSameColorAs(movingPiece)) {
                gameState.clearSelection();
                return;
            }

            int deltaRow = Math.abs(row - startRow);
            int deltaCol = Math.abs(col - startCol);
            int distance = Math.max(deltaRow, deltaCol);

            long arrivalTime = gameClock.getCurrentTime() + (distance * GameConfig.MOVE_TRAVEL_TIME_MS);
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

        boolean isAirborne = gameState.isPieceAirborne(move.getTargetRow(), move.getTargetCol(), move.getArrivalTime()) ||
                             gameState.isPieceAirborne(move.getTargetRow(), move.getTargetCol(), move.getArrivalTime() - 1);

        if (isAirborne) {
            String airbornePieceStr = board.getPieceAt(move.getTargetRow(), move.getTargetCol());
            
            if (!airbornePieceStr.equals(Piece.EMPTY)) {
                Piece airbornePiece = Piece.fromString(airbornePieceStr);
                Piece movingPiece = Piece.fromString(move.getPiece());

                if (!airbornePiece.isSameColorAs(movingPiece)) {
                    if (movingPiece.isKing()) {
                        gameState.endGame();
                    }
                    board.clearCell(move.getStartRow(), move.getStartCol());
                    return; 
                }
            }
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

    public IBoard getBoard() { return board; }
    public GameState getGameState() { return gameState; }
    public GameClock getGameClock() { return gameClock; }
}