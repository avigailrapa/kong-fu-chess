package src.view;

import src.model.Piece;

public record PieceSnapshot(String id, Piece.Color color, Piece.Kind kind, RenderState state,
                             int pixelX, int pixelY, long elapsedMillis) {

    public enum RenderState {
        IDLE, MOVING, JUMPING, LONG_REST, SHORT_REST, CAPTURED
    }
}
