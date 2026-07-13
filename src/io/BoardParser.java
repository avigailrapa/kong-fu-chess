package src.io;

import src.model.Board;
import src.model.Piece;
import src.model.Position;

public class BoardParser {

    public Board parse(String text) {
        String[] lines = splitNonBlankLines(text);
        if (lines.length == 0) {
            throw new IllegalArgumentException("Board text must contain at least one row");
        }

        String[][] rows = new String[lines.length][];
        int width = -1;
        for (int row = 0; row < lines.length; row++) {
            String[] cells = lines[row].trim().split("\\s+");
            if (width == -1) {
                width = cells.length;
            } else if (cells.length != width) {
                throw new IllegalArgumentException(
                        "Inconsistent row length at row " + row + ": expected " + width + " cells, got " + cells.length);
            }
            rows[row] = cells;
        }

        Board board = new Board(width, lines.length);
        int pieceCounter = 0;

        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < rows[row].length; col++) {
                String token = rows[row][col];
                if (token.equals(".")) {
                    continue;
                }
                Position position = new Position(row, col);
                Piece piece = createPiece(token, position, pieceCounter++);
                board.addPiece(piece, position);
            }
        }

        return board;
    }

    private Piece createPiece(String token, Position position, int index) {
        if (token.length() != 2) {
            throw new IllegalArgumentException("Invalid piece token: " + token);
        }
        Piece.Color color = parseColor(token.charAt(0));
        Piece.Kind kind = parseKind(token.charAt(1));
        return new Piece("p" + index, color, kind, position);
    }

    private Piece.Color parseColor(char c) {
        switch (c) {
            case 'w': return Piece.Color.WHITE;
            case 'b': return Piece.Color.BLACK;
            default: throw new IllegalArgumentException("Invalid piece color: " + c);
        }
    }

    private Piece.Kind parseKind(char c) {
        switch (c) {
            case 'K': return Piece.Kind.KING;
            case 'Q': return Piece.Kind.QUEEN;
            case 'R': return Piece.Kind.ROOK;
            case 'B': return Piece.Kind.BISHOP;
            case 'N': return Piece.Kind.KNIGHT;
            case 'P': return Piece.Kind.PAWN;
            default: throw new IllegalArgumentException("Invalid piece kind: " + c);
        }
    }

    private String[] splitNonBlankLines(String text) {
        return java.util.Arrays.stream(text.split("\\r?\\n"))
                .filter(line -> !line.trim().isEmpty())
                .toArray(String[]::new);
    }
}
