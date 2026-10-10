package org.tholian.alexa.tibber.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tholian.alexa.tibber.testsupport.FakeTibberServer;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.tholian.alexa.tibber.testsupport.TibberJson.homesResponse;
import static org.tholian.alexa.tibber.testsupport.TibberJson.unauthenticatedResponse;

/** KI-generiert: Token-Prüfung für das Account Linking (aus dem früheren TibberServiceTest übernommen). */
class TibberAccountServiceTest {

    private FakeTibberServer server;
    private TibberAccountService service;

    @BeforeEach
    void setUp() {
        server = new FakeTibberServer();
        service = new TibberAccountService(new TibberGraphQlClient(HttpClient.newHttpClient(), new ObjectMapper(),
                server.uri(), Duration.ofMillis(500)));
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void verifyToken_withHome_passesAndSendsMinimalQuery() {
        server.respondWith(200, homesResponse("home-1"));

        service.verifyToken("test-token");

        assertThat(server.lastAuthorization()).isEqualTo("Bearer test-token");
        // KI-generiert: Datensparsamkeit – die Prüfung fragt keine Preise oder Verbräuche ab
        assertThat(server.lastRequestBody()).contains("homes").doesNotContain("priceInfo").doesNotContain("consumption");
    }

    @Test
    void verifyToken_withoutHomes_throwsTibberApiException() {
        server.respondWith(200, homesResponse());

        assertThatThrownBy(() -> service.verifyToken("test-token"))
                .isExactlyInstanceOf(TibberApiException.class)
                .hasMessageContaining("Kein Zuhause");
    }

    @Test
    void verifyToken_withNullViewer_throwsTibberApiException() {
        server.respondWith(200, "{\"data\":{\"viewer\":null}}");

        assertThatThrownBy(() -> service.verifyToken("test-token"))
                .isExactlyInstanceOf(TibberApiException.class)
                .hasMessageContaining("unvollständig");
    }

    @Test
    void verifyToken_withRejectedToken_throwsAuthenticationException() {
        server.respondWith(200, unauthenticatedResponse());

        assertThatThrownBy(() -> service.verifyToken("test-token")).isInstanceOf(TibberAuthenticationException.class);
    }
}
