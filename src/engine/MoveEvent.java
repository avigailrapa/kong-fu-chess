package src.engine;

import src.model.Piece;
import src.model.Position;

public record MoveEvent(Piece.Color color, Piece.Kind kind, Position from, Position to,
                         boolean capture, boolean kingCapture, long requestTimestampMs) {

    public boolean isCapture() {
        return capture;
    }

    public boolean isKingCapture() {
        return kingCapture;
    }

    public String formattedRequestTime() {
        long totalMs = requestTimestampMs;
        long minutes = totalMs / 60000;
        long seconds = (totalMs % 60000) / 1000;
        long millis = totalMs % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }

    public String algebraicMove() {
        String prefix = kind == Piece.Kind.PAWN ? "" : kind.letter() + "";
        String destination = toFileRank(to);
        String captureMark = capture ? "x" : "";
        return prefix + captureMark + destination;
    }

    private String toFileRank(Position position) {
        char file = (char) ('a' + position.getCol());
        int rank = 8 - position.getRow();
        return "" + file + rank;
    }

    @Override
    public String toString() {
        return formattedRequestTime() + " " + algebraicMove();
    }
}
