package src.model;

import java.util.Optional;
import java.util.Set;

public interface IBoard {
    int width();
    int height();
    boolean isWithinBorder(Position position);
    Optional<Piece> pieceAt(Position position);
    void addPiece(Piece piece, Position position);
    void movePiece(Position from, Position to);
    Piece removePiece(Position position);
    Set<Position> occupiedPositions();
}
