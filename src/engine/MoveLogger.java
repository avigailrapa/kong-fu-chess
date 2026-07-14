package engine;

import src.model.Piece;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MoveLogger implements MoveObserver {
    private final List<MoveEvent> whiteMoves = new ArrayList<>();
    private final List<MoveEvent> blackMoves = new ArrayList<>();

    @Override
    public void onMove(MoveEvent moveEvent) {
        if (moveEvent.color() == Piece.Color.WHITE) {
            whiteMoves.add(moveEvent);
        } else {
            blackMoves.add(moveEvent);
        }
    }

    public List<MoveEvent> getWhiteMoves() {
        return Collections.unmodifiableList(whiteMoves);
    }

    public List<MoveEvent> getBlackMoves() {
        return Collections.unmodifiableList(blackMoves);
    }

    public void clear() {
        whiteMoves.clear();
        blackMoves.clear();
    }
}
