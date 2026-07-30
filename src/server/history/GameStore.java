package src.server.history;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class GameStore {

    private final Connection connection;

    public GameStore(String jdbcUrl) {
        try {
            this.connection = DriverManager.getConnection(jdbcUrl);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to connect to " + jdbcUrl, e);
        }
        createSchema();
    }

    private void createSchema() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS games (" +
                    "game_id TEXT PRIMARY KEY, " +
                    "white TEXT NOT NULL, " +
                    "black TEXT NOT NULL, " +
                    "winner TEXT NOT NULL, " +
                    "white_moves TEXT NOT NULL, " +
                    "black_moves TEXT NOT NULL, " +
                    "ended_at TIMESTAMP NOT NULL)");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize game store schema", e);
        }
    }

    public void save(String gameId, String white, String black, String winner, String whiteMoves,
                      String blackMoves) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO games (game_id, white, black, winner, white_moves, black_moves, ended_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, gameId);
            stmt.setString(2, white);
            stmt.setString(3, black);
            stmt.setString(4, winner);
            stmt.setString(5, whiteMoves);
            stmt.setString(6, blackMoves);
            stmt.setTimestamp(7, Timestamp.from(java.time.Instant.now()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save game " + gameId, e);
        }
    }

    public List<GameRecord> forUser(String username) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT game_id, white, black, winner, ended_at FROM games " +
                        "WHERE white = ? OR black = ? ORDER BY ended_at DESC")) {
            stmt.setString(1, username);
            stmt.setString(2, username);
            try (ResultSet rs = stmt.executeQuery()) {
                List<GameRecord> records = new ArrayList<>();
                while (rs.next()) {
                    records.add(new GameRecord(rs.getString("game_id"), rs.getString("white"),
                            rs.getString("black"), rs.getString("winner"), rs.getTimestamp("ended_at").toInstant()));
                }
                return records;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query games for " + username, e);
        }
    }
}
