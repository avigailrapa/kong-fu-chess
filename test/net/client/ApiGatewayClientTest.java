package net.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import src.net.client.ApiGatewayClient;
import src.net.client.TokenResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApiGatewayClientTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/login", this::handleLogin);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    public void stopServer() {
        server.stop(0);
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream in = exchange.getRequestBody()) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        boolean wrongPassword = body.contains("\"wrong\"");
        String response = wrongPassword ? "{\"error\":\"bad_credentials\"}" : "{\"rating\":1200,\"token\":\"tok-123\"}";
        int status = wrongPassword ? 401 : 200;
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Test
    public void testLoginReturnsTokenAndRatingOnSuccess() {
        ApiGatewayClient client = new ApiGatewayClient(baseUrl);

        TokenResult result = client.login("alice", "secret");

        assertTrue(result.accepted());
        assertEquals("tok-123", result.token());
        assertEquals(1200, result.rating());
    }

    @Test
    public void testLoginReturnsServerReportedRejectionReason() {
        ApiGatewayClient client = new ApiGatewayClient(baseUrl);

        TokenResult result = client.login("alice", "wrong");

        assertFalse(result.accepted());
        assertEquals("bad_credentials", result.reason());
    }

    @Test
    public void testLoginReturnsUnreachableWhenServerIsNotListening() {
        ApiGatewayClient client = new ApiGatewayClient("http://localhost:1");

        TokenResult result = client.login("alice", "secret");

        assertFalse(result.accepted());
        assertEquals("unreachable", result.reason());
    }
}
