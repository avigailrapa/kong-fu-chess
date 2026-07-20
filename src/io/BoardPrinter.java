package src.io;

import src.model.IBoard;
import src.model.Piece;
import src.model.Position;

import java.util.Optional;

public class BoardPrinter {

    public String print(IBoard board) {
        StringBuilder output = new StringBuilder();
        for (int row = 0; row < board.getHeight(); row++) {
            for (int col = 0; col < board.getWidth(); col++) {
                if (col > 0) {
                    output.append(' ');
                }
                output.append(tokenFor(board.getPieceAt(new Position(row, col))));
            }
            if (row < board.getHeight() - 1) {
                output.append('\n');
            }
        }
        return output.toString();
    }

    private String tokenFor(Optional<Piece> piece) {
        if (piece.isEmpty()) {
            return ".";
        }
        Piece p = piece.get();
        return "" + Character.toLowerCase(p.color().letter()) + p.kind().letter();
    }
}
