package src.net;

import src.engine.AlgebraicNotation;
import src.model.Piece;
import src.model.Position;
import src.view.GameSnapshot;
import src.view.PieceSnapshot;
import src.view.SelectionSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Protocol {

    private static final Pattern MOVE_PATTERN = Pattern.compile("^[WB][KQRBNP][a-h][1-8][a-h][1-8]$");
    private static final Pattern JUMP_PATTERN = Pattern.compile("^JUMP ([WB])([KQRBNP])([a-h][1-8])$");
    private static final String REJECT_PREFIX = "REJECT ";
    private static final String STATE_PREFIX = "STATE ";
    private static final String END_STATE = "ENDSTATE";

    private Protocol() {
    }

    public static WireMessage parse(String frameBody) {
        if (frameBody == null) {
            throw new MalformedMessageException("frame body must not be null");
        }
        if (frameBody.startsWith(STATE_PREFIX)) {
            return parseState(frameBody);
        }
        if (MOVE_PATTERN.matcher(frameBody).matches()) {
            return parseMove(frameBody);
        }
        Matcher jumpMatcher = JUMP_PATTERN.matcher(frameBody);
        if (jumpMatcher.matches()) {
            return parseJump(jumpMatcher);
        }
        if (frameBody.equals("OK")) {
            return new MoveAccepted();
        }
        if (frameBody.startsWith(REJECT_PREFIX)) {
            String reason = frameBody.substring(REJECT_PREFIX.length());
            if (reason.isBlank()) {
                throw new MalformedMessageException("REJECT requires a non-blank reason: " + frameBody);
            }
            return new MoveRejected(reason);
        }
        throw new MalformedMessageException("unrecognized message: " + frameBody);
    }

    public static String encode(WireMessage message) {
        if (message == null) {
            throw new MalformedMessageException("message must not be null");
        }
        return switch (message) {
            case MoveCommand m -> "" + m.color().letter() + m.kind().letter()
                    + AlgebraicNotation.toSquare(m.from()) + AlgebraicNotation.toSquare(m.to());
            case JumpCommand j -> "JUMP " + j.color().letter() + j.kind().letter()
                    + AlgebraicNotation.toSquare(j.at());
            case MoveAccepted ignored -> "OK";
            case MoveRejected r -> REJECT_PREFIX + r.reason();
            case StateMessage s -> encodeState(s);
        };
    }

    private static String encodeState(StateMessage message) {
        GameSnapshot snapshot = message.snapshot();
        StringBuilder sb = new StringBuilder();
        sb.append(STATE_PREFIX).append(snapshot.width()).append(' ').append(snapshot.height()).append(' ')
                .append(snapshot.gameOver() ? 1 : 0).append(' ')
                .append(snapshot.winner() == null ? "-" : String.valueOf(snapshot.winner().letter())).append(' ')
                .append(snapshot.whiteScore()).append(' ').append(snapshot.blackScore()).append(' ')
                .append(snapshot.zoom());

        for (int row = 0; row < snapshot.height(); row++) {
            for (int col = 0; col < snapshot.width(); col++) {
                PieceSnapshot piece = snapshot.pieceAt(new Position(row, col));
                if (piece == null) {
                    continue;
                }
                sb.append('\n').append("PIECE ").append(row).append(' ').append(col).append(' ')
                        .append(piece.id()).append(' ').append(piece.color().letter()).append(' ')
                        .append(piece.kind().letter()).append(' ').append(piece.state().name()).append(' ')
                        .append(piece.pixelX()).append(' ').append(piece.pixelY()).append(' ')
                        .append(piece.elapsedMillis()).append(' ').append(piece.restDurationMs());
            }
        }
        for (SelectionSnapshot selection : snapshot.selections()) {
            sb.append('\n').append("SELECT ").append(selection.playerId().letter()).append(' ')
                    .append(selection.position().row()).append(' ').append(selection.position().col());
        }
        for (Position destination : snapshot.legalDestinations()) {
            sb.append('\n').append("LEGAL ").append(destination.row()).append(' ').append(destination.col());
        }
        for (String entry : snapshot.whiteMoveLog()) {
            sb.append('\n').append("WLOG ").append(entry);
        }
        for (String entry : snapshot.blackMoveLog()) {
            sb.append('\n').append("BLOG ").append(entry);
        }
        sb.append('\n').append(END_STATE);
        return sb.toString();
    }

    private static WireMessage parseState(String frameBody) {
        try {
            String[] lines = frameBody.split("\n", -1);
            String[] header = lines[0].split(" ");
            if (header.length != 8) {
                throw new IllegalArgumentException("STATE header must have 8 fields: " + lines[0]);
            }
            int width = Integer.parseInt(header[1]);
            int height = Integer.parseInt(header[2]);
            boolean gameOver = header[3].equals("1");
            Piece.Color winner = header[4].equals("-") ? null : Piece.Color.fromLetter(header[4].charAt(0));
            int whiteScore = Integer.parseInt(header[5]);
            int blackScore = Integer.parseInt(header[6]);
            double zoom = Double.parseDouble(header[7]);

            PieceSnapshot[][] grid = new PieceSnapshot[height][width];
            List<SelectionSnapshot> selections = new ArrayList<>();
            Set<Position> legalDestinations = new HashSet<>();
            List<String> whiteMoveLog = new ArrayList<>();
            List<String> blackMoveLog = new ArrayList<>();

            boolean sawEndState = false;
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (line.equals(END_STATE)) {
                    sawEndState = true;
                    break;
                }
                if (line.startsWith("PIECE ")) {
                    String[] parts = line.split(" ");
                    int row = Integer.parseInt(parts[1]);
                    int col = Integer.parseInt(parts[2]);
                    grid[row][col] = new PieceSnapshot(parts[3], Piece.Color.fromLetter(parts[4].charAt(0)),
                            Piece.Kind.fromLetter(parts[5].charAt(0)), Piece.State.valueOf(parts[6]),
                            Integer.parseInt(parts[7]), Integer.parseInt(parts[8]),
                            Long.parseLong(parts[9]), Long.parseLong(parts[10]));
                } else if (line.startsWith("SELECT ")) {
                    String[] parts = line.split(" ");
                    selections.add(new SelectionSnapshot(Piece.Color.fromLetter(parts[1].charAt(0)),
                            new Position(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]))));
                } else if (line.startsWith("LEGAL ")) {
                    String[] parts = line.split(" ");
                    legalDestinations.add(new Position(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                } else if (line.startsWith("WLOG ")) {
                    whiteMoveLog.add(line.substring("WLOG ".length()));
                } else if (line.startsWith("BLOG ")) {
                    blackMoveLog.add(line.substring("BLOG ".length()));
                } else {
                    throw new IllegalArgumentException("unrecognized STATE line: " + line);
                }
            }
            if (!sawEndState) {
                throw new IllegalArgumentException("STATE block missing " + END_STATE);
            }

            GameSnapshot snapshot = new GameSnapshot(width, height, grid, selections, legalDestinations,
                    gameOver, winner, whiteScore, blackScore, whiteMoveLog, blackMoveLog, zoom);
            return new StateMessage(snapshot);
        } catch (RuntimeException e) {
            throw new MalformedMessageException("malformed STATE message", e);
        }
    }

    private static WireMessage parseMove(String frameBody) {
        try {
            Piece.Color color = Piece.Color.fromLetter(frameBody.charAt(0));
            Piece.Kind kind = Piece.Kind.fromLetter(frameBody.charAt(1));
            Position from = AlgebraicNotation.toPosition(frameBody.substring(2, 4));
            Position to = AlgebraicNotation.toPosition(frameBody.substring(4, 6));
            return new MoveCommand(color, kind, from, to);
        } catch (IllegalArgumentException e) {
            throw new MalformedMessageException("malformed move: " + frameBody, e);
        }
    }

    private static WireMessage parseJump(Matcher jumpMatcher) {
        try {
            Piece.Color color = Piece.Color.fromLetter(jumpMatcher.group(1).charAt(0));
            Piece.Kind kind = Piece.Kind.fromLetter(jumpMatcher.group(2).charAt(0));
            Position at = AlgebraicNotation.toPosition(jumpMatcher.group(3));
            return new JumpCommand(color, kind, at);
        } catch (IllegalArgumentException e) {
            throw new MalformedMessageException("malformed jump: " + jumpMatcher.group(), e);
        }
    }
}
