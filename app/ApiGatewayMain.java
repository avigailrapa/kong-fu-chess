package app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import redis.clients.jedis.JedisPooled;
import src.server.auth.RedisTokenStore;
import src.server.auth.TokenStore;
import src.server.auth.UserRecord;
import src.server.auth.UserStore;
import src.server.health.HealthServer;
import src.server.history.GameRecord;
import src.server.history.GameStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiGatewayMain {

    private static final String DEFAULT_POSTGRES_URL =
            "jdbc:postgresql://localhost:5432/kongfu?user=kongfu&password=kongfu";
    private static final String DEFAULT_REDIS_URL = "redis://localhost:6379";
    static final String DEFAULT_PORT = "8080";
    private static final String DEFAULT_HEALTH_PORT = "8085";
    private static final Pattern USERNAME_FIELD = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern PASSWORD_FIELD = Pattern.compile("\"password\"\\s*:\\s*\"([^\"]*)\"");

    public static void main(String[] args) throws Exception {
        String postgresUrl = AppSupport.env("POSTGRES_URL", DEFAULT_POSTGRES_URL);
        String redisUrl = AppSupport.env("REDIS_URL", DEFAULT_REDIS_URL);
        int port = Integer.parseInt(AppSupport.env("API_PORT", DEFAULT_PORT));
        int healthPort = Integer.parseInt(AppSupport.env("HEALTH_PORT", DEFAULT_HEALTH_PORT));

        UserStore userStore = new UserStore(postgresUrl);
        GameStore gameStore = new GameStore(postgresUrl);
        JedisPooled redis = new JedisPooled(URI.create(redisUrl));
        TokenStore tokenStore = new RedisTokenStore(redis);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/login", exchange -> handleLogin(exchange, userStore, tokenStore));
        server.createContext("/history/", exchange -> handleHistory(exchange, gameStore));
        server.start();

        new HealthServer(healthPort, Map::of).start();

        System.out.println("KongFu api-gateway listening on port " + port);
        Thread.currentThread().join();
    }

    private static void handleLogin(HttpExchange exchange, UserStore userStore, TokenStore tokenStore)
            throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        String body = readBody(exchange);
        Optional<String> username = extractField(USERNAME_FIELD, body);
        Optional<String> password = extractField(PASSWORD_FIELD, body);
        if (username.isEmpty() || password.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"malformed_request\"}");
            return;
        }

        Optional<UserRecord> existing = userStore.find(username.get());
        UserRecord user;
        if (existing.isPresent()) {
            if (!userStore.checkPassword(username.get(), password.get())) {
                sendJson(exchange, 401, "{\"error\":\"bad_credentials\"}");
                return;
            }
            user = existing.get();
        } else {
            user = userStore.createUser(username.get(), password.get());
        }
        String token = tokenStore.mint(user.username());
        sendJson(exchange, 200, "{\"rating\":" + user.rating() + ",\"token\":\"" + token + "\"}");
    }

    private static void handleHistory(HttpExchange exchange, GameStore gameStore) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String username = path.substring("/history/".length());
        if (username.isBlank()) {
            sendJson(exchange, 400, "{\"error\":\"missing_username\"}");
            return;
        }
        List<GameRecord> games = gameStore.forUser(username);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < games.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            GameRecord g = games.get(i);
            json.append("{\"gameId\":\"").append(g.gameId()).append("\",")
                    .append("\"white\":\"").append(g.white()).append("\",")
                    .append("\"black\":\"").append(g.black()).append("\",")
                    .append("\"winner\":\"").append(g.winner()).append("\",")
                    .append("\"endedAt\":\"").append(g.endedAt()).append("\"}");
        }
        json.append("]");
        sendJson(exchange, 200, json.toString());
    }

    private static Optional<String> extractField(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
