package src.realtime;

import src.model.Piece;
import src.model.Position;

public class ArrivalEvent {
    private final Piece movedPiece;
    private final Position from;
    private final Position to;
    private final Piece capturedPiece;
    private final boolean kingCaptured;

    public ArrivalEvent(Piece movedPiece, Position from, Position to, Piece capturedPiece, boolean kingCaptured) {
        this.movedPiece = movedPiece;
        this.from = from;
        this.to = to;
        this.capturedPiece = capturedPiece;
        this.kingCaptured = kingCaptured;
    }

    public Piece movedPiece() {
        return movedPiece;
    }

    public Position from() {
        return from;
    }

    public Position to() {
        return to;
    }

    public Piece capturedPiece() {
        return capturedPiece;
    }

    public boolean kingCaptured() {
        return kingCaptured;
    }
}