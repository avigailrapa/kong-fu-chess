package src.server.history;

import java.time.Instant;

public record GameRecord(String gameId, String white, String black, String winner, Instant endedAt) {
}
