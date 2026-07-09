package model;

import java.util.Optional;

public interface IBoard {
    int getWidth();
    int getHeight();
    boolean isWithinBorder(Position position);
    Optional<Piece> getPieceAt(Position position);
    void addPiece(Piece piece, Position position);
    void movePiece(Position from, Position to);
    Piece removePiece(Position position);
}
