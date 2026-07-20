package src.engine;

import src.model.Piece;
import src.model.Position;

public record MoveEvent(Piece.Color color, Piece.Kind kind, Position from, Position to,
                         boolean capture, boolean kingCapture, long requestTimestampMs) {

    public String formattedRequestTime() {
        long totalMs = requestTimestampMs;
        long minutes = totalMs / 60000;
        long seconds = (totalMs % 60000) / 1000;
        long millis = totalMs % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }

    public String algebraicMove() {
        String prefix = kind == Piece.Kind.PAWN ? "" : kind.letter() + "";
        String destination = AlgebraicNotation.toSquare(to);
        String captureMark = capture ? "x" : "";
        return prefix + captureMark + destination;
    }

    @Override
    public String toString() {
        return formattedRequestTime() + " " + algebraicMove();
    }
}
