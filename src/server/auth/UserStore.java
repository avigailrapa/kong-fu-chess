package src.server.auth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class UserStore {

    private static final int DEFAULT_RATING = 1200;

    private final Connection connection;

    public UserStore(String jdbcUrl) {
        try {
            this.connection = DriverManager.getConnection(jdbcUrl);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to connect to " + jdbcUrl, e);
        }
        createSchema();
    }

    private void createSchema() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY, " +
                    "password_hash TEXT NOT NULL, " +
                    "password_salt TEXT NOT NULL, " +
                    "rating INTEGER NOT NULL DEFAULT " + DEFAULT_RATING + ")");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize user store schema", e);
        }
    }

    public Optional<UserRecord> find(String username) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT rating FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(new UserRecord(username, rs.getInt("rating"))) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query user " + username, e);
        }
    }

    public UserRecord createUser(String username, String password) {
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash(password, salt);
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO users (username, password_hash, password_salt, rating) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, username);
            stmt.setString(2, hash);
            stmt.setString(3, salt);
            stmt.setInt(4, DEFAULT_RATING);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create user " + username, e);
        }
        return new UserRecord(username, DEFAULT_RATING);
    }

    public boolean checkPassword(String username, String password) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT password_hash, password_salt FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String expectedHash = rs.getString("password_hash");
                String salt = rs.getString("password_salt");
                return PasswordHasher.hash(password, salt).equals(expectedHash);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check password for " + username, e);
        }
    }

    public void updateRating(String username, int newRating) {
        try (PreparedStatement stmt = connection.prepareStatement("UPDATE users SET rating = ? WHERE username = ?")) {
            stmt.setInt(1, newRating);
            stmt.setString(2, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update rating for " + username, e);
        }
    }
}
