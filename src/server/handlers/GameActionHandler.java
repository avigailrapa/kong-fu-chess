package src.server.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import src.engine.MoveResult;
import src.model.Piece;
import src.model.Position;
import src.net.Protocol;
import src.net.messages.JumpCommand;
import src.net.messages.MoveAccepted;
import src.net.messages.MoveCommand;
import src.net.messages.MoveRejected;
import src.net.messages.SelectCommand;
import src.server.core.Match;
import src.server.core.Session;
import src.server.core.SessionRegistry;
import src.view.snapshot.PieceSnapshot;

public class GameActionHandler {

    private static final Logger log = LoggerFactory.getLogger(GameActionHandler.class);

    private final SessionRegistry sessionRegistry;

    public GameActionHandler(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public String handleMove(Object conn, MoveCommand m) {
        String rejection = validateSeatedAction(conn, m.color(), m.kind(), m.from());
        if (rejection != null) {
            log.warn("illegal move rejected: {}", rejection);
            return Protocol.encode(new MoveRejected(rejection));
        }
        MoveResult result = sessionRegistry.matchFor(conn).engine().requestMove(m.from(), m.to());
        return result.isAccepted()
                ? Protocol.encode(new MoveAccepted())
                : Protocol.encode(new MoveRejected(result.reason()));
    }

    public String handleJump(Object conn, JumpCommand j) {
        String rejection = validateSeatedAction(conn, j.color(), j.kind(), j.at());
        if (rejection != null) {
            log.warn("illegal jump rejected: {}", rejection);
            return Protocol.encode(new MoveRejected(rejection));
        }
        sessionRegistry.matchFor(conn).engine().requestJump(j.at());
        return Protocol.encode(new MoveAccepted());
    }

    public String handleSelect(Object conn, SelectCommand sel) {
        Session session = sessionRegistry.sessionFor(conn);
        if (session != null) {
            session.selectedCell(sel.selected());
        }
        return Protocol.encode(new MoveAccepted());
    }

    private String validateSeatedAction(Object conn, Piece.Color color, Piece.Kind kind, Position position) {
        if (isSpectator(conn)) {
            return "spectator";
        }
        Match match = sessionRegistry.matchFor(conn);
        if (match == null || !ownsColor(conn, color)) {
            return "not_your_piece";
        }
        if (!declaredTokenMatchesBoard(match, position, color, kind)) {
            return "token_mismatch";
        }
        return null;
    }

    private boolean ownsColor(Object conn, Piece.Color declaredColor) {
        Session session = sessionRegistry.sessionFor(conn);
        return session != null && session.assignedColor() == declaredColor;
    }

    private boolean isSpectator(Object conn) {
        Session session = sessionRegistry.sessionFor(conn);
        return session != null && session.role() == Session.Role.SPECTATOR;
    }

    private boolean declaredTokenMatchesBoard(Match match, Position position, Piece.Color color,
                                              Piece.Kind kind) {
        PieceSnapshot piece = match.engine().snapshot(null).pieceAt(position);
        return piece != null && piece.color() == color && piece.kind() == kind;
    }
}
