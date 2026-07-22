package view;

import org.junit.jupiter.api.Test;
import src.model.Piece;
import src.view.GameSnapshot;
import src.view.PieceSnapshot;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class GameSnapshotTest {

    private GameSnapshot snapshotWithOnePieceAt(int row, int col, int pixelX, int pixelY, double zoom) {
        PieceSnapshot[][] board = new PieceSnapshot[8][8];
        board[row][col] = new PieceSnapshot("w-P-1", Piece.Color.WHITE, Piece.Kind.PAWN, Piece.State.IDLE,
                pixelX, pixelY, 0, 0);
        return new GameSnapshot(8, 8, board, List.of(), Set.of(), false, null, 0, 0,
                List.of(), List.of(), zoom);
    }

    @Test
    public void testWithZoomScalesPiecePixelPositionsProportionally() {
        GameSnapshot original = snapshotWithOnePieceAt(2, 3, 300, 200, 1.0);

        GameSnapshot rescaled = original.withZoom(2.0);

        PieceSnapshot piece = rescaled.pieceAt(new src.model.Position(2, 3));
        assertEquals(2.0, rescaled.zoom());
        assertEquals(600, piece.pixelX());
        assertEquals(400, piece.pixelY());
    }

    @Test
    public void testWithZoomAtSameZoomReturnsSameInstance() {
        GameSnapshot original = snapshotWithOnePieceAt(0, 0, 100, 100, 1.0);

        GameSnapshot result = original.withZoom(1.0);

        assertSame(original, result);
    }

    @Test
    public void testWithZoomPreservesEmptySquares() {
        GameSnapshot original = snapshotWithOnePieceAt(4, 4, 400, 400, 1.0);

        GameSnapshot rescaled = original.withZoom(0.5);

        assertNull(rescaled.pieceAt(new src.model.Position(0, 0)));
    }
}
