package server;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.framing.Framedata;
import org.java_websocket.protocols.IProtocol;

import javax.net.ssl.SSLSession;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * A connection identity for tests that exercise {@code GameServer.handleMessage(WebSocket, String)}
 * directly, without a real socket. Every method throws except the ones needed to behave as a plain
 * Map key (identity equals/hashCode, inherited from Object) - GameServer.handleMessage never calls
 * back into the connection itself, only GameServer.onMessage does, which these tests bypass.
 */
public class FakeWebSocket implements WebSocket {
    @Override
    public void close(int code, String message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close(int code) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void closeConnection(int code, String message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(String text) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(ByteBuffer bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(byte[] bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendFrame(Framedata framedata) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendFrame(Collection<Framedata> frames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendPing() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendFragmentedFrame(org.java_websocket.enums.Opcode op, ByteBuffer buffer, boolean fin) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasBufferedData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosing() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFlushAndClose() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Draft getDraft() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ReadyState getReadyState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getResourceDescriptor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> void setAttachment(T attachment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getAttachment() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasSSLSupport() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SSLSession getSSLSession() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IProtocol getProtocol() {
        throw new UnsupportedOperationException();
    }
}
