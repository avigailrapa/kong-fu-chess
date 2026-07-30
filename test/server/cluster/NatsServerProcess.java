package server.cluster;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NatsServerProcess {

    private static final String DEFAULT_EXECUTABLE = "tools/nats-server.exe";
    private static final Pattern PORT_PATTERN =
            Pattern.compile("Listening for client connections on \\S+:(\\d+)");

    private final Process process;
    private final int port;

    private NatsServerProcess(Process process, int port) {
        this.process = process;
        this.port = port;
    }

    public static NatsServerProcess start() throws IOException {
        File executable = new File(System.getenv().getOrDefault("NATS_SERVER_PATH", DEFAULT_EXECUTABLE));
        if (!executable.isFile()) {
            throw new IllegalStateException("nats-server executable not found at " + executable.getAbsolutePath()
                    + " - fetch nats-server.exe from https://github.com/nats-io/nats-server/releases into "
                    + "tools/nats-server.exe, or point NATS_SERVER_PATH at an existing install");
        }
        Process process = new ProcessBuilder(executable.getAbsolutePath(), "-p", "-1")
                .redirectErrorStream(true)
                .start();
        int port = readBoundPort(process);
        return new NatsServerProcess(process, port);
    }

    private static int readBoundPort(Process process) throws IOException {
        CompletableFuture<Integer> portFuture = new CompletableFuture<>();
        Thread reader = new Thread(() -> {
            try (BufferedReader out = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = out.readLine()) != null) {
                    Matcher matcher = PORT_PATTERN.matcher(line);
                    if (matcher.find()) {
                        portFuture.complete(Integer.parseInt(matcher.group(1)));
                    }
                }
            } catch (IOException e) {
                portFuture.completeExceptionally(e);
            }
        }, "nats-server-log-reader");
        reader.setDaemon(true);
        reader.start();
        try {
            return portFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            process.destroyForcibly();
            throw new IllegalStateException("nats-server did not report a bound port in time", e);
        }
    }

    public String url() {
        return "nats://localhost:" + port;
    }

    public void stop() {
        process.destroy();
        try {
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
