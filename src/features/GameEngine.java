package features;


public class GameEngine {
    private static final long MOVE_TRAVEL_TIME = 1000L;
    private static final long JUMP_DURATION = 1000L;

    private final Board board;
    private final GameState gameState;
    private final GameClock gameClock;
    private final MoveValidator moveValidator;

    public GameEngine(Board board) {
        this.board = board;
        this.gameState = new GameState();
        this.gameClock = new GameClock();
        this.moveValidator = new MoveValidator(board);
    }

    public void handleClick(int pixelX, int pixelY) {
        if (gameState.isGameOver()) {
            return;
        }

        updateBoardState();

        int col = pixelX / 100;
        int row = pixelY / 100;

        if (!board.isWithinBounds(row, col)) {
            return;
        }

        String targetCell = board.getPieceAt(row, col);

        if (gameState.hasSelection()) {
            processMoveAttempt(row, col, targetCell);
        } else {
            processSelection(row, col, targetCell);
        }
    }

    public void handleJump(int pixelX, int pixelY) {
        if (gameState.isGameOver()) {
            return;
        }

        updateBoardState();

        int col = pixelX / 100;
        int row = pixelY / 100;

        if (!board.isWithinBounds(row, col)) {
            return;
        }

        String targetCell = board.getPieceAt(row, col);

        if (!targetCell.equals(".") && !gameState.isPieceInFlight(row, col) 
                && !gameState.isPieceAirborne(row, col, gameClock.getCurrentTime())) {
            gameState.addJump(new JumpState(row, col, gameClock.getCurrentTime(), 
                                           gameClock.getCurrentTime() + JUMP_DURATION));
        }
    }

    public void advanceTime(long milliseconds) {
        gameClock.advance(milliseconds);
        updateBoardState();
    }

    public void updateBoardState() {
        processPendingMoves();
        gameState.cleanupCompletedJumps(gameClock.getCurrentTime());
    }

    public void printBoard() {
        updateBoardState();
        board.printBoard();
    }


    private void processSelection(int row, int col, String targetCell) {
        if (!targetCell.equals(".") && !gameState.isPieceInFlight(row, col)) {
            gameState.selectPiece(row, col);
        }
    }

    private void processMoveAttempt(int row, int col, String targetCell) {
        int selectedRow = gameState.getSelectedRow();
        int selectedCol = gameState.getSelectedCol();

        if (row == selectedRow && col == selectedCol) {
            gameState.clearSelection();
            return;
        }

        String sourcePiece = board.getPieceAt(selectedRow, selectedCol);
        char sourceColor = sourcePiece.charAt(0);
        char pieceType = sourcePiece.charAt(1);

        if (!targetCell.equals(".") && targetCell.charAt(0) == sourceColor) {
            if (!gameState.isPieceInFlight(row, col)) {
                gameState.selectPiece(row, col);
            }
            return;
        }

        if (isLegalMove(pieceType, sourceColor, selectedRow, selectedCol, row, col)) {
            executeMove(selectedRow, selectedCol, row, col, sourcePiece);
            gameState.clearSelection();
        } else {
            gameState.clearSelection();
        }
    }

    private boolean isLegalMove(char pieceType, char sourceColor, int startRow, int startCol, 
                               int targetRow, int targetCol) {
        if (!moveValidator.isValidMove(pieceType, sourceColor, startRow, startCol, targetRow, targetCol)) {
            return false;
        }

        if (gameState.hasActiveMoveOfOppositeColor(sourceColor)) {
            return false;
        }

        return !gameState.isTargetClaimed(targetRow, targetCol);
    }

    private void executeMove(int startRow, int startCol, int targetRow, int targetCol, String piece) {
        long arrivalTime = gameClock.getCurrentTime() + MOVE_TRAVEL_TIME;
        gameState.addPendingMove(new PendingMove(startRow, startCol, targetRow, targetCol, piece, arrivalTime));
    }

    private void processPendingMoves() {
        for (PendingMove move : gameState.getPendingMoves()) {
            if (move.hasArrived(gameClock.getCurrentTime())) {
                applyMove(move);
                gameState.removePendingMove(move);
            }
        }
    }

    private void applyMove(PendingMove move) {
        JumpState interceptingJump = gameState.findActiveJumpAt(move.getTargetRow(), move.getTargetCol(), 
                                                               move.getArrivalTime());
        if (interceptingJump != null) {
            if (Piece.fromString(move.getPiece()).isKing()) {
                gameState.endGame();
            }
            board.clearCell(move.getStartRow(), move.getStartCol());
            return;
        }

        String capturedPiece = board.getPieceAt(move.getTargetRow(), move.getTargetCol());
        if (!capturedPiece.equals(".") && Piece.fromString(capturedPiece).isKing()) {
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

    public Board getBoard() {
        return board;
    }

    public GameState getGameState() {
        return gameState;
    }

    public GameClock getGameClock() {
        return gameClock;
    }
}
