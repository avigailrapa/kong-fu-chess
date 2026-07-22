package src.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Board implements IBoard {

    private final int width;
    private final int height;
    private final Map<Position, Piece> occupied;

    public Board(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Board dimensions must be positive: width=" + width + ", height=" + height);
        }
        this.width = width;
        this.height = height;
        this.occupied = new HashMap<>();
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public boolean isWithinBorder(Position position) {
        return position.row() >= 0 && position.row() < height
                && position.col() >= 0 && position.col() < width;
    }

    @Override
    public Optional<Piece> pieceAt(Position position) {
        requireWithinBorder(position);
        return Optional.ofNullable(occupied.get(position));
    }

    @Override
    public void addPiece(Piece piece, Position position) {
        requireWithinBorder(position);
        if (occupied.containsKey(position)) {
            throw new IllegalStateException("Cell already occupied: " + position);
        }
        if (hasPieceWithId(piece.id())) {
            throw new IllegalStateException("Duplicate piece id: " + piece.id());
        }
        occupied.put(position, piece);
        piece.setCell(position);
    }

    @Override
    public void movePiece(Position from, Position to) {
        requireWithinBorder(from);
        requireWithinBorder(to);

        Piece piece = occupied.get(from);
        if (piece == null) {
            throw new IllegalStateException("No piece at source position: " + from);
        }
        if (occupied.containsKey(to)) {
            throw new IllegalStateException("Target cell already occupied: " + to);
        }

        occupied.remove(from);
        occupied.put(to, piece);
        piece.setCell(to);
    }

    @Override
    public Piece removePiece(Position position) {
        requireWithinBorder(position);
        Piece removed = occupied.remove(position);
        if (removed == null) {
            throw new IllegalStateException("No piece to remove at: " + position);
        }
        return removed;
    }

    @Override
    public Set<Position> occupiedPositions() {
        return Set.copyOf(occupied.keySet());
    }

    private void requireWithinBorder(Position position) {
        if (!isWithinBorder(position)) {
            throw new IllegalArgumentException("Position out of bounds: " + position);
        }
    }

    private boolean hasPieceWithId(String id) {
        return occupied.values().stream().anyMatch(p -> p.id().equals(id));
    }
}
