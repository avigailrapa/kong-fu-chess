package src.view;

import src.model.Piece;

public record PieceSnapshot(String id, Piece.Color color, Piece.Kind kind, Piece.State state,
                             int pixelX, int pixelY, long elapsedMillis, long restDurationMs) {
}
