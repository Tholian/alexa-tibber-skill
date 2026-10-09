package org.tholian.alexa.tibber.testsupport;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * KI-generiert: Minimaler HTTP-Server als Ersatz für die Tibber GraphQL API in Tests.
 * Läuft auf einem freien Port auf der Loopback-Adresse, liefert eine konfigurierbare
 * Antwort und merkt sich den letzten Request (Authorization-Header + Body) für Assertions.
 * Ersetzt das frühere Mocking des HTTP-Clients durch echtes HTTP –
 * damit werden Serialisierung, Header und Timeouts realistisch mitgetestet.
 */
public final class FakeTibberServer implements AutoCloseable {

    private final HttpServer server;
    private final AtomicInteger requestCount = new AtomicInteger();
    private volatile int status = 200;
    private volatile String responseBody = "{}";
    private volatile Duration delay = Duration.ZERO;
    private volatile Duration bodyDelay = Duration.ZERO;
    private volatile String lastAuthorization;
    private volatile String lastRequestBody;

    public FakeTibberServer() {
        try {
            // KI-generiert: Port 0 = freier Port, nur Loopback → nicht aus dem Netz erreichbar
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/gql", exchange -> {
            requestCount.incrementAndGet();
            lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            pause(delay);
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                // KI-generiert: Header sind bereits raus (volle Content-Length), der Body folgt erst nach der Pause
                out.flush();
                pause(bodyDelay);
                out.write(bytes);
            }
        });
        server.start();
    }

    /** KI-generiert: Basis-URL des Fake-Endpoints, z. B. {@code http://127.0.0.1:54321/gql}. */
    public URI uri() {
        var address = server.getAddress();
        return URI.create("http://" + address.getHostString() + ":" + address.getPort() + "/gql");
    }

    /** KI-generiert: Legt Status und Body für alle folgenden Requests fest. */
    public void respondWith(int status, String body) {
        this.status = status;
        this.responseBody = body;
    }

    /** KI-generiert: Verzögert jede Antwort – zum Testen der Client-Timeouts. */
    public void delayResponsesBy(Duration delay) {
        this.delay = delay;
    }

    /**
     * KI-generiert: Sendet die Header sofort (mit voller Content-Length) und schreibt den Body erst nach
     * der Pause – zum Testen eines langsamen Bodys, den ein reiner Header-Timeout nicht erfasst.
     */
    public void delayBodyBy(Duration delay) {
        this.bodyDelay = delay;
    }

    public String lastAuthorization() {
        return lastAuthorization;
    }

    public String lastRequestBody() {
        return lastRequestBody;
    }

    public int requestCount() {
        return requestCount.get();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static void pause(Duration duration) {
        if (duration.isZero()) {
            return;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
