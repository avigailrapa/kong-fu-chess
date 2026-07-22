package src.view.snapshot;

import src.model.Piece;
import src.model.Position;

import java.util.List;
import java.util.Set;

public record GameSnapshot(int width, int height, PieceSnapshot[][] board, List<SelectionSnapshot> selections,
                            Set<Position> legalDestinations,
                            boolean gameOver, Piece.Color winner, int whiteScore, int blackScore,
                            List<String> whiteMoveLog, List<String> blackMoveLog, double zoom) {

    public GameSnapshot {
        board = copyBoard(board, height, width);
        selections = List.copyOf(selections);
        legalDestinations = Set.copyOf(legalDestinations);
        whiteMoveLog = List.copyOf(whiteMoveLog);
        blackMoveLog = List.copyOf(blackMoveLog);
    }

    private static PieceSnapshot[][] copyBoard(PieceSnapshot[][] board, int height, int width) {
        PieceSnapshot[][] copy = new PieceSnapshot[height][width];
        for (int row = 0; row < height; row++) {
            System.arraycopy(board[row], 0, copy[row], 0, width);
        }
        return copy;
    }

    private static final double BOARD_SCALE = 0.9;
    private static final double BOARD_IMAGE_WIDTH_PX = 822.0;
    private static final double BOARD_IMAGE_HEIGHT_PX = 828.0;
    private static final int STANDARD_BOARD_DIMENSION = 8;
    public static final double CELL_WIDTH = BOARD_IMAGE_WIDTH_PX * BOARD_SCALE / STANDARD_BOARD_DIMENSION;
    public static final double CELL_HEIGHT = BOARD_IMAGE_HEIGHT_PX * BOARD_SCALE / STANDARD_BOARD_DIMENSION;

    public static final double MIN_ZOOM = 0.5;
    public static final double MAX_ZOOM = 2.0;
    public static final double DEFAULT_ZOOM = 1.0;

    public double cellWidth() {
        return CELL_WIDTH * zoom;
    }

    public double cellHeight() {
        return CELL_HEIGHT * zoom;
    }

    public PieceSnapshot pieceAt(Position position) {
        return board[position.row()][position.col()];
    }

    public boolean isOccupied(Position position) {
        return pieceAt(position) != null;
    }

    public GameSnapshot withZoom(double newZoom) {
        if (newZoom == zoom) {
            return this;
        }
        double scale = newZoom / zoom;
        PieceSnapshot[][] rescaledBoard = new PieceSnapshot[height][width];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                PieceSnapshot piece = board[row][col];
                if (piece == null) {
                    continue;
                }
                rescaledBoard[row][col] = new PieceSnapshot(piece.id(), piece.color(), piece.kind(), piece.state(),
                        (int) Math.round(piece.pixelX() * scale), (int) Math.round(piece.pixelY() * scale),
                        piece.elapsedMillis(), piece.restDurationMs());
            }
        }
        return new GameSnapshot(width, height, rescaledBoard, selections, legalDestinations, gameOver, winner,
                whiteScore, blackScore, whiteMoveLog, blackMoveLog, newZoom);
    }
}
