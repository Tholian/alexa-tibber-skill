package org.tholian.alexa.tibber.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tholian.alexa.tibber.model.TibberResponse;
import org.tholian.alexa.tibber.testsupport.FakeTibberServer;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.tholian.alexa.tibber.testsupport.TibberJson.errorResponse;
import static org.tholian.alexa.tibber.testsupport.TibberJson.homesResponse;
import static org.tholian.alexa.tibber.testsupport.TibberJson.unauthenticatedResponse;

/**
 * KI-generiert: Tests für den HTTP-Transport zur Tibber GraphQL API gegen einen lokalen FakeTibberServer.
 * Die Fehler- und Timeout-Fälle stammen aus dem früheren TibberServiceTest.
 * Sicherheitshinweis: ausschließlich Fantasie-Tokens, kein Netzwerkzugriff nach außen.
 */
class TibberGraphQlClientTest {

    private static final String QUERY = "{ viewer { homes { id } } }";

    private FakeTibberServer server;
    private TibberGraphQlClient client;

    @BeforeEach
    void setUp() {
        server = new FakeTibberServer();
        client = new TibberGraphQlClient(HttpClient.newHttpClient(), new ObjectMapper(), server.uri(), Duration.ofMillis(500));
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void query_sendsBearerTokenAndQuery() {
        server.respondWith(200, homesResponse("home-1"));

        client.query("test-token", QUERY);

        assertThat(server.lastAuthorization()).isEqualTo("Bearer test-token");
        assertThat(server.lastRequestBody()).contains("\"query\"").contains("homes");
    }

    @Test
    void query_withoutVariables_omitsVariablesField() {
        server.respondWith(200, homesResponse("home-1"));

        client.query("test-token", QUERY);

        assertThat(server.lastRequestBody()).doesNotContain("variables");
    }

    @Test
    void query_withVariables_sendsThemAsJsonObject() {
        server.respondWith(200, homesResponse("home-1"));

        client.query("test-token", "query($last: Int!) { viewer { homes { id } } }", Map.of("last", 3));

        // KI-generiert: SICHERHEIT – Werte gehen als variables-Objekt, nicht als Teil des Query-Strings
        assertThat(server.lastRequestBody()).contains("\"variables\":{\"last\":3}");
    }

    @Test
    void query_withZeroTimeout_failsWithoutRequest() {
        server.respondWith(200, homesResponse("home-1"));

        assertThatThrownBy(() -> client.query("test-token", QUERY, null, Duration.ZERO))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("Zeitbudget");
        assertThat(server.requestCount()).isZero();
    }

    @Test
    void query_withShortTimeoutOverride_timesOutEarlierThanDefault() {
        server.respondWith(200, homesResponse("home-1"));
        server.delayResponsesBy(Duration.ofMillis(300));

        assertThatThrownBy(() -> client.query("test-token", QUERY, null, Duration.ofMillis(100)))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("nicht erreichbar");
    }

    @Test
    void query_withHttpError_throwsTibberApiException() {
        server.respondWith(500, "{}");

        assertThatThrownBy(() -> client.query("test-token", QUERY))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("HTTP 500");
    }

    @Test
    void query_withGraphQlErrors_throwsWithMessageButWithoutToken() {
        server.respondWith(200, errorResponse("Context creation failed: invalid token"));

        assertThatThrownBy(() -> client.query("secret-token-123", QUERY))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("invalid token")
                .message().doesNotContain("secret-token-123");
    }

    @Test
    void query_withInvalidJson_throwsTibberApiException() {
        server.respondWith(200, "not-json");

        assertThatThrownBy(() -> client.query("test-token", QUERY))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("nicht lesbar");
    }

    @Test
    void query_whenServerTooSlow_throwsTibberApiException() {
        // KI-generiert: Request-Timeout im Test = 500 ms, Server antwortet erst nach 2 s
        server.respondWith(200, homesResponse("home-1"));
        server.delayResponsesBy(Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.query("test-token", QUERY))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("nicht erreichbar");
    }

    @Test
    void query_whenBodyTooSlow_throwsTibberApiException() {
        // KI-generiert: Header kommen sofort, der Body erst nach 2 s – der Gesamt-Timeout (500 ms) muss greifen
        server.respondWith(200, homesResponse("home-1"));
        server.delayBodyBy(Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.query("test-token", QUERY))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("nicht erreichbar");
    }

    @Test
    void query_withHttp401_throwsAuthenticationException() {
        server.respondWith(401, "{}");

        assertThatThrownBy(() -> client.query("test-token", QUERY)).isInstanceOf(TibberAuthenticationException.class);
    }

    @Test
    void query_withHttp403_throwsAuthenticationException() {
        server.respondWith(403, "{}");

        assertThatThrownBy(() -> client.query("test-token", QUERY)).isInstanceOf(TibberAuthenticationException.class);
    }

    @Test
    void query_withGraphQlUnauthenticated_throwsAuthenticationException() {
        server.respondWith(200, unauthenticatedResponse());

        assertThatThrownBy(() -> client.query("secret-token-123", QUERY))
                .isInstanceOf(TibberAuthenticationException.class)
                .message().doesNotContain("secret-token-123");
    }

    @Test
    void query_withHttp400AndUnauthenticatedBody_throwsAuthenticationException() {
        server.respondWith(400, unauthenticatedResponse());

        assertThatThrownBy(() -> client.query("test-token", QUERY)).isInstanceOf(TibberAuthenticationException.class);
    }

    @Test
    void query_withOtherGraphQlError_isNoAuthenticationError() {
        server.respondWith(200, errorResponse("Something else went wrong"));

        assertThatThrownBy(() -> client.query("test-token", QUERY)).isExactlyInstanceOf(TibberApiException.class);
    }

    @Test
    void firstHome_returnsFirstHome() {
        server.respondWith(200, homesResponse("home-1", "home-2"));

        TibberResponse.Home home = TibberGraphQlClient.firstHome(client.query("test-token", QUERY));

        assertThat(home.id()).isEqualTo("home-1");
    }

    @Test
    void firstHome_withoutHomes_throws() {
        server.respondWith(200, homesResponse());

        assertThatThrownBy(() -> TibberGraphQlClient.firstHome(client.query("test-token", QUERY)))
                .isExactlyInstanceOf(TibberApiException.class)
                .hasMessageContaining("Kein Zuhause");
    }

    @Test
    void firstHome_withNullViewer_throws() {
        server.respondWith(200, "{\"data\":{\"viewer\":null}}");

        assertThatThrownBy(() -> TibberGraphQlClient.firstHome(client.query("test-token", QUERY)))
                .isExactlyInstanceOf(TibberApiException.class)
                .hasMessageContaining("unvollständig");
    }

    @Test
    void firstHome_withNullHomes_throws() {
        server.respondWith(200, "{\"data\":{\"viewer\":{\"homes\":null}}}");

        assertThatThrownBy(() -> TibberGraphQlClient.firstHome(client.query("test-token", QUERY)))
                .isExactlyInstanceOf(TibberApiException.class)
                .hasMessageContaining("unvollständig");
    }
}
