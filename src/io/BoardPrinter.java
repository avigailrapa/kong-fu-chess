package io;

import model.IBoard;
import model.Piece;
import model.Position;

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
        return "" + colorChar(p.getColor()) + kindChar(p.getKind());
    }

    private char colorChar(Piece.Color color) {
        return color == Piece.Color.WHITE ? 'w' : 'b';
    }

    private char kindChar(Piece.Kind kind) {
        switch (kind) {
            case KING: return 'K';
            case QUEEN: return 'Q';
            case ROOK: return 'R';
            case BISHOP: return 'B';
            case KNIGHT: return 'N';
            case PAWN: return 'P';
            default: throw new IllegalStateException("Unknown piece kind: " + kind);
        }
    }
}
