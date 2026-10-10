package org.tholian.alexa.tibber.testsupport;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.enums.Opcode;
import org.java_websocket.extensions.IExtension;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.protocols.IProtocol;
import org.java_websocket.protocols.Protocol;
import org.java_websocket.server.WebSocketServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * KI-generiert: Minimaler GraphQL-over-WebSocket-Server (Protokoll graphql-transport-ws) als Ersatz für die
 * Tibber-Live-API in Tests. Läuft auf Loopback mit Zufallsport, Verhalten per {@link Behavior} skriptbar,
 * merkt sich empfangene Nachrichten und Handshake-Header für Assertions.
 * Basiert auf org.java-websocket (nur testImplementation – nicht im Lambda-Paket).
 */
public final class FakeTibberWebSocketServer implements AutoCloseable {

    public enum Behavior { MEASUREMENT, PING_THEN_MEASUREMENT, FRAGMENTED_MEASUREMENT, ERROR, CLOSE_ON_SUBSCRIBE, NO_ACK }

    /** KI-generiert: Messwerte, die bei MEASUREMENT gesendet werden (1240 W, heute 6,3 kWh für 1,72 €). */
    public static final String MEASUREMENT = """
            {"timestamp":"2026-10-10T10:00:00+02:00","power":1240.0,"powerProduction":0.0,\
            "accumulatedConsumption":6.3,"accumulatedCost":1.72,"currency":"EUR"}""";

    private final Server server;
    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch clientClosed = new CountDownLatch(1);
    private final List<String> received = new CopyOnWriteArrayList<>();
    private final AtomicInteger connections = new AtomicInteger();
    private volatile Behavior behavior = Behavior.MEASUREMENT;
    private volatile String userAgent;
    private volatile String subprotocol;

    public FakeTibberWebSocketServer() {
        // KI-generiert: Port 0 = freier Port, nur Loopback → nicht aus dem Netz erreichbar
        server = new Server(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        server.setReuseAddr(true);
        server.start();
        if (!await(started, Duration.ofSeconds(5))) {
            throw new IllegalStateException("FakeTibberWebSocketServer startet nicht");
        }
    }

    public URI uri() {
        return URI.create("ws://127.0.0.1:" + server.getPort() + "/v1-beta/gql/subscriptions");
    }

    public void behave(Behavior behavior) {
        this.behavior = behavior;
    }

    public List<String> received() {
        return List.copyOf(received);
    }

    public String userAgent() {
        return userAgent;
    }

    public String subprotocol() {
        return subprotocol;
    }

    public int connectionCount() {
        return connections.get();
    }

    /** KI-generiert: Wartet, bis die (erste) Client-Verbindung geschlossen wurde. */
    public boolean awaitClientClosed(Duration timeout) {
        return await(clientClosed, timeout);
    }

    @Override
    public void close() {
        try {
            server.stop(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean await(CountDownLatch latch, Duration timeout) {
        try {
            return latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String next(String measurement) {
        return "{\"id\":\"1\",\"type\":\"next\",\"payload\":{\"data\":{\"liveMeasurement\":" + measurement + "}}}";
    }

    private final class Server extends WebSocketServer {

        Server(InetSocketAddress address) {
            // KI-generiert: Nur das Subprotokoll graphql-transport-ws wird akzeptiert
            super(address, List.<Draft>of(new Draft_6455(Collections.<IExtension>emptyList(),
                    List.<IProtocol>of(new Protocol("graphql-transport-ws")))));
        }

        @Override
        public void onStart() {
            started.countDown();
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            connections.incrementAndGet();
            userAgent = handshake.getFieldValue("User-Agent");
            subprotocol = handshake.getFieldValue("Sec-WebSocket-Protocol");
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            received.add(message);
            if (message.contains("\"connection_init\"")) {
                if (behavior != Behavior.NO_ACK) {
                    conn.send("{\"type\":\"connection_ack\"}");
                }
                return;
            }
            if (!message.contains("\"subscribe\"")) {
                return;
            }
            switch (behavior) {
                case MEASUREMENT -> conn.send(next(MEASUREMENT));
                case PING_THEN_MEASUREMENT -> {
                    conn.send("{\"type\":\"ping\"}");
                    conn.send(next(MEASUREMENT));
                }
                case FRAGMENTED_MEASUREMENT -> {
                    String text = next(MEASUREMENT);
                    int half = text.length() / 2;
                    conn.sendFragmentedFrame(Opcode.TEXT,
                            ByteBuffer.wrap(text.substring(0, half).getBytes(StandardCharsets.UTF_8)), false);
                    conn.sendFragmentedFrame(Opcode.TEXT,
                            ByteBuffer.wrap(text.substring(half).getBytes(StandardCharsets.UTF_8)), true);
                }
                case ERROR -> conn.send("{\"id\":\"1\",\"type\":\"error\",\"payload\":[{\"message\":\"forbidden\"}]}");
                case CLOSE_ON_SUBSCRIBE -> conn.close(4403, "Forbidden");
                case NO_ACK -> {
                    // KI-generiert: kommt nicht vor – ohne ack sendet der Client kein subscribe
                }
            }
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            clientClosed.countDown();
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            // KI-generiert: Fehler einzelner Testverbindungen sind für die Assertions irrelevant
        }
    }
}
