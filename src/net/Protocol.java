package src.net;

import src.engine.AlgebraicNotation;
import src.model.Piece;
import src.model.Position;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Protocol {

    private static final Pattern MOVE_PATTERN = Pattern.compile("^[WB][KQRBNP][a-h][1-8][a-h][1-8]$");
    private static final Pattern JUMP_PATTERN = Pattern.compile("^JUMP ([WB])([KQRBNP])([a-h][1-8])$");
    private static final String REJECT_PREFIX = "REJECT ";

    private Protocol() {
    }

    public static WireMessage parse(String frameBody) {
        if (frameBody == null) {
            throw new MalformedMessageException("frame body must not be null");
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
        };
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
