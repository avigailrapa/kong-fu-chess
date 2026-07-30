package src.server.health;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class HealthServer {

    private final int port;
    private final Supplier<Map<String, Object>> gauges;
    private final long startedAtMs = System.currentTimeMillis();
    private HttpServer httpServer;

    public HealthServer(int port, Supplier<Map<String, Object>> gauges) {
        this.port = port;
        this.gauges = gauges;
    }

    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/health", exchange -> {
            byte[] body = toJson().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        httpServer.start();
    }

    private String toJson() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("status", "ok");
        fields.put("uptimeMs", System.currentTimeMillis() - startedAtMs);
        fields.putAll(gauges.get());
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            json.append(value instanceof String ? "\"" + value + "\"" : String.valueOf(value));
        }
        return json.append("}").toString();
    }
}
