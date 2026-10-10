package org.tholian.alexa.tibber.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.tholian.alexa.tibber.model.LiveMeasurement;
import org.tholian.alexa.tibber.testsupport.FakeTibberServer;
import org.tholian.alexa.tibber.testsupport.FakeTibberWebSocketServer;
import org.tholian.alexa.tibber.testsupport.FakeTibberWebSocketServer.Behavior;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.tholian.alexa.tibber.testsupport.TibberJson.array;
import static org.tholian.alexa.tibber.testsupport.TibberJson.consumptionNode;
import static org.tholian.alexa.tibber.testsupport.TibberJson.livePrerequisiteResponse;

/**
 * KI-generiert: Live-Daten mit HTTP-Vorabfrage (FakeTibberServer) und WebSocket (FakeTibberWebSocketServer).
 * Jeder Fehler im Live-Pfad führt zur Antwort mit der letzten abgerechneten Stunde.
 */
class LiveServiceTest {

    private static final String TOKEN = "live-secret-token-4711";
    private static final String LAST_HOUR = array(consumptionNode("2026-10-10T09:00:00+02:00", 0.84, 0.21));

    private FakeTibberServer http;
    private FakeTibberWebSocketServer ws;
    private LiveService service;

    @BeforeEach
    void setUp() {
        http = new FakeTibberServer();
        ws = new FakeTibberWebSocketServer();
        var client = new TibberGraphQlClient(HttpClient.newHttpClient(), new ObjectMapper(), http.uri(), Duration.ofMillis(500));
        service = new LiveService(client, new LiveMeasurementClient(client.httpClient(), client.objectMapper()));
    }

    @AfterEach
    void tearDown() {
        http.close();
        ws.close();
    }

    @Test
    void currentUsage_withPulse_returnsLiveMeasurement() {
        http.respondWith(200, livePrerequisiteResponse(ws.uri().toString(), true, LAST_HOUR));

        CurrentUsage usage = service.currentUsage(TOKEN, Deadline.after(Duration.ofSeconds(3)));

        assertThat(usage).isInstanceOfSatisfying(CurrentUsage.Live.class,
                live -> assertThat(live.measurement().power()).isEqualTo(1240.0));
        // KI-generiert: Datensparsamkeit – keine personenbezogenen Felder in der Vorabfrage
        assertThat(http.lastRequestBody()).contains("websocketSubscriptionUrl").contains("realTimeConsumptionEnabled")
                .doesNotContain("address").doesNotContain("owner");
    }

    @Test
    void currentUsage_withoutPulse_returnsLastHourWithoutWebSocket() {
        http.respondWith(200, livePrerequisiteResponse(ws.uri().toString(), false, LAST_HOUR));

        CurrentUsage usage = service.currentUsage(TOKEN, Deadline.after(Duration.ofSeconds(3)));

        assertThat(usage).isInstanceOfSatisfying(CurrentUsage.LastHour.class,
                lastHour -> assertThat(lastHour.node().consumption()).isEqualTo(0.84));
        assertThat(ws.connectionCount()).isZero();
    }

    @Test
    void currentUsage_withForeignWebSocketUrl_neverConnectsAndFallsBack() {
        assertNoConnectionAttempt("wss://evil.example.com/subscriptions");
    }

    @Test
    void currentUsage_withUserinfoInWebSocketUrl_neverConnectsAndFallsBack() {
        assertNoConnectionAttempt("ws://x@127.0.0.1:" + ws.uri().getPort() + "/subscriptions");
    }

    /** Zählendes Test-Double (echtes Objekt): jeder Verbindungsversuch wird gezählt und scheitert ohne Netzzugriff. */
    private static final class CountingLiveClient extends LiveMeasurementClient {

        private int calls;

        CountingLiveClient(HttpClient httpClient, ObjectMapper objectMapper) {
            super(httpClient, objectMapper);
        }

        @Override
        public LiveMeasurement firstMeasurement(URI url, String token, String homeId, Deadline deadline) {
            calls++;
            throw new TibberApiException("kein Netzzugriff im Test");
        }
    }

    private void assertNoConnectionAttempt(String webSocketUrl) {
        var client = new TibberGraphQlClient(HttpClient.newHttpClient(), new ObjectMapper(), http.uri(), Duration.ofMillis(500));
        var counting = new CountingLiveClient(client.httpClient(), client.objectMapper());
        http.respondWith(200, livePrerequisiteResponse(webSocketUrl, true, LAST_HOUR));

        CurrentUsage usage = new LiveService(client, counting).currentUsage(TOKEN, Deadline.after(Duration.ofSeconds(3)));

        assertThat(usage).isInstanceOf(CurrentUsage.LastHour.class);
        assertThat(counting.calls).as("firstMeasurement-Aufrufe").isZero();
    }

    @Test
    void currentUsage_whenNoMeasurementInBudget_fallsBackToLastHour() {
        http.respondWith(200, livePrerequisiteResponse(ws.uri().toString(), true, LAST_HOUR));
        ws.behave(Behavior.NO_ACK);
        long start = System.nanoTime();

        CurrentUsage usage = service.currentUsage(TOKEN, Deadline.after(Duration.ofMillis(800)));

        assertThat(usage).isInstanceOf(CurrentUsage.LastHour.class);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
        assertThat(ws.awaitClientClosed(Duration.ofSeconds(2))).isTrue();
    }

    @Test
    void currentUsage_whenSubscriptionFails_fallsBackToLastHour() {
        http.respondWith(200, livePrerequisiteResponse(ws.uri().toString(), true, LAST_HOUR));
        ws.behave(Behavior.ERROR);

        assertThat(service.currentUsage(TOKEN, Deadline.after(Duration.ofSeconds(3)))).isInstanceOf(CurrentUsage.LastHour.class);
    }

    @Test
    void currentUsage_withoutConsumptionNodes_returnsEmptyLastHour() {
        http.respondWith(200, livePrerequisiteResponse(ws.uri().toString(), false, "[]"));

        assertThat(service.currentUsage(TOKEN, Deadline.after(Duration.ofSeconds(3))))
                .isEqualTo(new CurrentUsage.LastHour(null));
    }

    @Test
    void currentUsage_whenTokenRejected_throwsAuthenticationException() {
        http.respondWith(401, "{}");

        assertThatThrownBy(() -> service.currentUsage(TOKEN, Deadline.after(Duration.ofSeconds(3))))
                .isInstanceOf(TibberAuthenticationException.class);
    }

    @Test
    void currentUsage_neverLogsToken() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        root.addAppender(appender);
        try {
            http.respondWith(200, livePrerequisiteResponse(ws.uri().toString(), true, LAST_HOUR));
            ws.behave(Behavior.ERROR);
            service.currentUsage(TOKEN, Deadline.after(Duration.ofSeconds(2)));
            ws.behave(Behavior.CLOSE_ON_SUBSCRIBE);
            service.currentUsage(TOKEN, Deadline.after(Duration.ofSeconds(2)));
            ws.behave(Behavior.NO_ACK);
            service.currentUsage(TOKEN, Deadline.after(Duration.ofMillis(500)));
        } finally {
            root.detachAppender(appender);
        }

        // KI-generiert: Nur Ereignisse des Skills prüfen – Bibliotheken des Test-Servers sind nicht Teil des Lambda-Pakets
        assertThat(appender.list).filteredOn(event -> event.getLoggerName().startsWith("org.tholian"))
                .isNotEmpty()
                .allSatisfy(event -> {
                    assertThat(event.getFormattedMessage()).doesNotContain(TOKEN);
                    for (IThrowableProxy proxy = event.getThrowableProxy(); proxy != null; proxy = proxy.getCause()) {
                        assertThat(String.valueOf(proxy.getMessage())).doesNotContain(TOKEN);
                    }
                });
    }
}
