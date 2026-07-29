package server.auth;

import src.server.auth.UserStore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class TestDatabase {

    public static final String JDBC_URL = System.getenv().getOrDefault("TEST_POSTGRES_URL",
            "jdbc:postgresql://localhost:5432/kongfu?user=kongfu&password=kongfu");

    private TestDatabase() {
    }

    public static UserStore freshUserStore() {
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS users");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to reset test database at " + JDBC_URL, e);
        }
        return new UserStore(JDBC_URL);
    }
}
