package app;

import io.nats.client.Connection;
import io.nats.client.Nats;
import src.server.cluster.WsGateway;

import java.net.InetSocketAddress;

public class WsGatewayMain {

    static final int DEFAULT_PORT = 8887;
    private static final String DEFAULT_NATS_URL = "nats://localhost:4222";
    private static final long BIND_TIMEOUT_MS = 5000;
    private static final long POLL_INTERVAL_MS = 20;

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(AppSupport.env("WS_PORT", String.valueOf(DEFAULT_PORT)));
        String natsUrl = AppSupport.env("NATS_URL", DEFAULT_NATS_URL);

        Connection nats = Nats.connect(natsUrl);
        WsGateway gateway = new WsGateway(new InetSocketAddress(port), nats);
        gateway.start();
        if (!waitForBoundPort(gateway)) {
            System.err.println("ws-gateway failed to bind to port " + port + " within " + BIND_TIMEOUT_MS + "ms");
            System.exit(1);
        }
        System.out.println("KongFu ws-gateway listening on port " + port + ", relaying to NATS at " + natsUrl);
    }

    private static boolean waitForBoundPort(WsGateway gateway) throws InterruptedException {
        long deadline = System.currentTimeMillis() + BIND_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (gateway.getPort() > 0) {
                return true;
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        return false;
    }
}
