package src.server.handlers;

import src.engine.GameOverEvent;
import src.model.Piece;
import src.net.Protocol;
import src.net.messages.RatingChanged;
import src.server.auth.UserStore;
import src.server.core.EloCalculator;
import src.server.core.Match;
import src.server.core.Session;

import java.util.List;

public class RatingService {

    private final UserStore userStore;
    private final MatchBroadcaster broadcaster;

    public RatingService(UserStore userStore, MatchBroadcaster broadcaster) {
        this.userStore = userStore;
        this.broadcaster = broadcaster;
    }

    public void updateRatingsAfterGameOver(Match match, GameOverEvent event) {
        List<Session> seated = match.seated();
        Session white = seated.stream().filter(s -> s.assignedColor() == Piece.Color.WHITE).findFirst().orElse(null);
        Session black = seated.stream().filter(s -> s.assignedColor() == Piece.Color.BLACK).findFirst().orElse(null);
        if (white == null || black == null) {
            return;
        }
        double whiteScore = event.winner() == null ? 0.5 : event.winner() == Piece.Color.WHITE ? 1.0 : 0.0;
        int newWhiteRating = EloCalculator.updatedRating(white.rating(), black.rating(), whiteScore);
        int newBlackRating = EloCalculator.updatedRating(black.rating(), white.rating(), 1.0 - whiteScore);
        userStore.updateRating(white.username(), newWhiteRating);
        userStore.updateRating(black.username(), newBlackRating);
        white.rating(newWhiteRating);
        black.rating(newBlackRating);
        broadcaster.sendQuietly(white, Protocol.encode(new RatingChanged(newWhiteRating)));
        broadcaster.sendQuietly(black, Protocol.encode(new RatingChanged(newBlackRating)));
    }
}
