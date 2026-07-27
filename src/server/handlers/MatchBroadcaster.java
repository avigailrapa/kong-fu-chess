package src.server.handlers;

import src.engine.GameOverEvent;
import src.engine.MoveEvent;
import src.model.Position;
import src.net.Protocol;
import src.net.messages.GameOverMessage;
import src.net.messages.MoveOccurred;
import src.net.messages.StateMessage;
import src.server.core.ActivityLog;
import src.server.core.Match;
import src.server.core.Session;
import src.view.snapshot.GameSnapshot;
import src.view.snapshot.PieceSnapshot;

public class MatchBroadcaster {

    private final ActivityLog activityLog;

    public MatchBroadcaster(ActivityLog activityLog) {
        this.activityLog = activityLog;
    }

    public void broadcastState(Match match) {
        GameSnapshot boardState = match.engine().snapshot(null);
        for (Session session : match.seated()) {
            sendQuietly(session, encodedState(match, ownSelection(session, boardState)));
        }
        for (Session spectator : match.spectators()) {
            sendQuietly(spectator, encodedState(match, null));
        }
    }

    public void broadcastMoveEvent(Match match, MoveEvent event) {
        broadcastToMatch(match, Protocol.encode(new MoveOccurred(event)));
    }

    public void broadcastGameOver(Match match, GameOverEvent event) {
        activityLog.log("game over - winner: " + (event.winner() == null ? "draw" : event.winner()));
        broadcastToMatch(match, Protocol.encode(new GameOverMessage(event)));
    }

    private void broadcastToMatch(Match match, String text) {
        for (Session session : match.seated()) {
            sendQuietly(session, text);
        }
        for (Session spectator : match.spectators()) {
            sendQuietly(spectator, text);
        }
    }

    public void sendQuietly(Session session, String text) {
        logOutgoing(text);
        try {
            session.connection().send(text);
        } catch (RuntimeException e) {
        }
    }

    private void logOutgoing(String text) {
        if (!text.startsWith("STATE ")) {
            activityLog.log("SERVER_TO_CLIENT " + text);
        }
    }

    private Position ownSelection(Session session, GameSnapshot boardState) {
        if (session == null || session.selectedCell() == null) {
            return null;
        }
        PieceSnapshot piece = boardState.pieceAt(session.selectedCell());
        return piece != null && piece.color() == session.assignedColor() ? session.selectedCell() : null;
    }

    private String encodedState(Match match, Position selectedCell) {
        GameSnapshot snapshot = match.engine().snapshot(selectedCell,
                match.moveLogger().whiteMoves().stream().map(MoveEvent::toString).toList(),
                match.moveLogger().blackMoves().stream().map(MoveEvent::toString).toList());
        return Protocol.encode(new StateMessage(snapshot));
    }
}
