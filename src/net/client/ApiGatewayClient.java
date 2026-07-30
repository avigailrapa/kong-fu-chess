package src.net.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiGatewayClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Pattern TOKEN_FIELD = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern RATING_FIELD = Pattern.compile("\"rating\"\\s*:\\s*(\\d+)");
    private static final Pattern ERROR_FIELD = Pattern.compile("\"error\"\\s*:\\s*\"([^\"]*)\"");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String baseUrl;

    public ApiGatewayClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public TokenResult login(String username, String password) {
        String body = "{\"username\":\"" + escape(username) + "\",\"password\":\"" + escape(password) + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/login"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return toTokenResult(response);
        } catch (IOException e) {
            return new TokenResult(false, 0, null, "unreachable");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new TokenResult(false, 0, null, "unreachable");
        }
    }

    private TokenResult toTokenResult(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            String reason = extractField(ERROR_FIELD, response.body()).orElse("http_" + response.statusCode());
            return new TokenResult(false, 0, null, reason);
        }
        Optional<String> token = extractField(TOKEN_FIELD, response.body());
        if (token.isEmpty()) {
            return new TokenResult(false, 0, null, "malformed_response");
        }
        int rating = extractField(RATING_FIELD, response.body()).map(Integer::parseInt).orElse(0);
        return new TokenResult(true, rating, token.get(), "ok");
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Optional<String> extractField(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
