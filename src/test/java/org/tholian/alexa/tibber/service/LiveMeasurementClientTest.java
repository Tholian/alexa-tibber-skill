package org.tholian.alexa.tibber.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tholian.alexa.tibber.model.LiveMeasurement;
import org.tholian.alexa.tibber.testsupport.FakeTibberWebSocketServer;
import org.tholian.alexa.tibber.testsupport.FakeTibberWebSocketServer.Behavior;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * KI-generiert: GraphQL-Subscription über echtes WebSocket gegen einen lokalen Fake-Server.
 * Sicherheitshinweis: ausschließlich Fantasie-Token, nur Loopback.
 */
class LiveMeasurementClientTest {

    private static final String TOKEN = "live-secret-token-4711";

    private FakeTibberWebSocketServer server;
    private LiveMeasurementClient client;

    @BeforeEach
    void setUp() {
        server = new FakeTibberWebSocketServer();
        client = new LiveMeasurementClient(HttpClient.newHttpClient(), new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void firstMeasurement_speaksGraphqlTransportWsAndReturnsMeasurement() {
        LiveMeasurement measurement = client.firstMeasurement(server.uri(), TOKEN, "home-1", Deadline.after(Duration.ofSeconds(3)));

        assertThat(measurement.power()).isEqualTo(1240.0);
        assertThat(measurement.accumulatedConsumption()).isEqualTo(6.3);
        assertThat(measurement.accumulatedCost()).isEqualTo(1.72);
        assertThat(measurement.currency()).isEqualTo("EUR");
        assertThat(server.userAgent()).isEqualTo("alexa-tibber-skill/1.0");
        assertThat(server.subprotocol()).isEqualTo("graphql-transport-ws");
        assertThat(server.received().get(0)).contains("\"type\":\"connection_init\"").contains(TOKEN);
        assertThat(server.received().get(1)).contains("\"type\":\"subscribe\"").contains("liveMeasurement")
                .contains("\"homeId\":\"home-1\"").doesNotContain(TOKEN);
        assertThat(server.awaitClientClosed(Duration.ofSeconds(2))).isTrue();
    }

    @Test
    void firstMeasurement_answersPingWithPong() {
        server.behave(Behavior.PING_THEN_MEASUREMENT);

        client.firstMeasurement(server.uri(), TOKEN, "home-1", Deadline.after(Duration.ofSeconds(3)));

        assertThat(server.awaitClientClosed(Duration.ofSeconds(2))).isTrue();
        assertThat(server.received()).anySatisfy(message -> assertThat(message).contains("\"type\":\"pong\""));
    }

    @Test
    void firstMeasurement_withFragmentedMessage_reassemblesIt() {
        server.behave(Behavior.FRAGMENTED_MEASUREMENT);

        LiveMeasurement measurement = client.firstMeasurement(server.uri(), TOKEN, "home-1", Deadline.after(Duration.ofSeconds(3)));

        assertThat(measurement.power()).isEqualTo(1240.0);
    }

    @Test
    void firstMeasurement_whenServerSendsError_throwsWithoutToken() {
        server.behave(Behavior.ERROR);

        assertThatThrownBy(() -> client.firstMeasurement(server.uri(), TOKEN, "home-1", Deadline.after(Duration.ofSeconds(3))))
                .isInstanceOf(TibberApiException.class)
                .message().doesNotContain(TOKEN);
    }

    @Test
    void firstMeasurement_whenServerClosesAfterAck_throws() {
        server.behave(Behavior.CLOSE_ON_SUBSCRIBE);

        assertThatThrownBy(() -> client.firstMeasurement(server.uri(), TOKEN, "home-1", Deadline.after(Duration.ofSeconds(3))))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("geschlossen");
    }

    @Test
    void firstMeasurement_withoutAck_timesOutWithinBudgetAndCloses() {
        server.behave(Behavior.NO_ACK);
        long start = System.nanoTime();

        assertThatThrownBy(() -> client.firstMeasurement(server.uri(), TOKEN, "home-1", Deadline.after(Duration.ofMillis(300))))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("Zeitbudget");

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(1500));
        assertThat(server.awaitClientClosed(Duration.ofSeconds(2))).isTrue();
    }

    @Test
    void firstMeasurement_withExhaustedBudget_doesNotConnect() {
        assertThatThrownBy(() -> client.firstMeasurement(server.uri(), TOKEN, "home-1", Deadline.after(Duration.ZERO)))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("Zeitbudget");
        assertThat(server.connectionCount()).isZero();
    }

    @Test
    void firstMeasurement_whenServerUnreachable_throws() {
        // KI-generiert: Port 1 auf Loopback ist nicht belegt → Verbindung wird abgelehnt
        URI unreachable = URI.create("ws://127.0.0.1:1/v1-beta/gql/subscriptions");

        assertThatThrownBy(() -> client.firstMeasurement(unreachable, TOKEN, "home-1", Deadline.after(Duration.ofSeconds(2))))
                .isInstanceOf(TibberApiException.class);
    }
}
