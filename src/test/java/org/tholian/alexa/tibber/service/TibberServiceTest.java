package org.tholian.alexa.tibber.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tholian.alexa.tibber.model.ConsumptionNode;
import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.testsupport.FakeTibberServer;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.tholian.alexa.tibber.testsupport.TibberJson.array;
import static org.tholian.alexa.tibber.testsupport.TibberJson.errorResponse;
import static org.tholian.alexa.tibber.testsupport.TibberJson.homeResponse;
import static org.tholian.alexa.tibber.testsupport.TibberJson.price;

/**
 * KI-generiert: Unit-Tests für TibberService gegen einen lokalen FakeTibberServer (echtes HTTP, kein Mock).
 * Die Uhr ist fest auf 2026-06-15T10:00:00Z gestellt, damit Vergangenheits-/Zukunftsfilter deterministisch sind.
 *
 * Sicherheitshinweis: Es werden ausschließlich Fantasie-Tokens verwendet; kein Netzwerkzugriff nach außen.
 */
class TibberServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");
    private static final String PAST = "2026-06-15T09:00:00Z";
    private static final String FUTURE_1 = "2026-06-15T11:00:00Z";
    private static final String FUTURE_2 = "2026-06-15T12:00:00Z";

    private FakeTibberServer server;
    private TibberService service;

    @BeforeEach
    void setUp() {
        server = new FakeTibberServer();
        service = new TibberService(HttpClient.newHttpClient(), new ObjectMapper(), server.uri(),
                Duration.ofMillis(500), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    // -------------------------------------------------------------------------
    // Formatierung
    // -------------------------------------------------------------------------

    @Test
    void formatPrice_convertsEuroToCent() {
        assertThat(service.formatPrice(0.25)).isEqualTo("25,0 Cent");
    }

    @Test
    void formatPrice_zeroEuro_returnsZeroCent() {
        assertThat(service.formatPrice(0.0)).isEqualTo("0,0 Cent");
    }

    @Test
    void formatTime_convertsIsoStringToBerlinTimeSummer() {
        // KI-generiert: 2026-06-15 liegt sicher in CEST (UTC+2), UTC 10:00 = Berlin 12:00
        assertThat(service.formatTime("2026-06-15T10:00:00Z")).isEqualTo("12:00 Uhr");
    }

    @Test
    void formatTime_convertsIsoStringToBerlinTimeWinter() {
        // KI-generiert: 2026-01-15 liegt sicher in CET (UTC+1), UTC 11:00 = Berlin 12:00
        assertThat(service.formatTime("2026-01-15T11:00:00Z")).isEqualTo("12:00 Uhr");
    }

    @Test
    void formatLevel_mapsAllKnownLevels() {
        assertThat(service.formatLevel("VERY_CHEAP")).isEqualTo("sehr günstig");
        assertThat(service.formatLevel("CHEAP")).isEqualTo("günstig");
        assertThat(service.formatLevel("NORMAL")).isEqualTo("normal");
        assertThat(service.formatLevel("EXPENSIVE")).isEqualTo("teuer");
        assertThat(service.formatLevel("VERY_EXPENSIVE")).isEqualTo("sehr teuer");
    }

    @Test
    void formatLevel_unknown_returnsLowercase() {
        assertThat(service.formatLevel("UNKNOWN_LEVEL")).isEqualTo("unknown_level");
    }

    @Test
    void formatLevel_null_returnsUnbekannt() {
        assertThat(service.formatLevel(null)).isEqualTo("unbekannt");
    }

    // -------------------------------------------------------------------------
    // API-Methoden
    // -------------------------------------------------------------------------

    @Test
    void getCurrentPrice_returnsCurrentPriceFromResponse() {
        server.respondWith(200, homeResponse(price(0.30, "EXPENSIVE", PAST), "[]", "null", "[]"));

        Price result = service.getCurrentPrice("test-token");

        assertThat(result.total()).isEqualTo(0.30);
        assertThat(result.level()).isEqualTo("EXPENSIVE");
    }

    @Test
    void getCurrentPrice_sendsBearerTokenAndGraphQlQuery() {
        server.respondWith(200, homeResponse(price(0.30, "EXPENSIVE", PAST), "[]", "null", "[]"));

        service.getCurrentPrice("test-token");

        assertThat(server.lastAuthorization()).isEqualTo("Bearer test-token");
        assertThat(server.lastRequestBody()).contains("\"query\"").contains("priceInfo");
    }

    @Test
    void getPriceList_combinesTodayAndTomorrow() {
        server.respondWith(200, homeResponse("null",
                array(price(0.20, "CHEAP", "2026-06-15T10:00:00Z")),
                array(price(0.35, "EXPENSIVE", "2026-06-16T10:00:00Z")),
                "[]"));

        List<Price> result = service.getPriceList("test-token");

        assertThat(result).extracting(Price::startsAt)
                .containsExactlyInAnyOrder("2026-06-15T10:00:00Z", "2026-06-16T10:00:00Z");
    }

    @Test
    void getPriceList_withNullTomorrow_returnsOnlyToday() {
        server.respondWith(200, homeResponse("null", array(price(0.20, "CHEAP", PAST)), "null", "[]"));

        assertThat(service.getPriceList("test-token")).hasSize(1);
    }

    @Test
    void getCurrentConsumption_withNode_returnsOptionalOfNode() {
        server.respondWith(200, homeResponse("null", "[]", "null",
                "[{\"from\":\"2026-06-15T09:00:00Z\",\"to\":\"2026-06-15T10:00:00Z\",\"consumption\":0.5,\"cost\":0.125}]"));

        Optional<ConsumptionNode> result = service.getCurrentConsumption("test-token");

        assertThat(result).isPresent();
        assertThat(result.get().consumption()).isEqualTo(0.5);
        assertThat(result.get().cost()).isEqualTo(0.125);
    }

    @Test
    void getCurrentConsumption_withEmptyNodes_returnsEmpty() {
        server.respondWith(200, homeResponse("null", "[]", "null", "[]"));

        assertThat(service.getCurrentConsumption("test-token")).isEmpty();
    }

    @Test
    void getCurrentConsumption_withoutConsumptionBlock_returnsEmpty() {
        server.respondWith(200, """
                {"data":{"viewer":{"homes":[{"currentSubscription":null,"consumption":null}]}}}
                """);

        assertThat(service.getCurrentConsumption("test-token")).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Filter-Methoden (Uhr fest auf NOW)
    // -------------------------------------------------------------------------

    @Test
    void getNextLowestPrice_ignoresPastPrices_returnsLowestFuturePrice() {
        server.respondWith(200, homeResponse("null", array(
                price(0.10, "VERY_CHEAP", PAST),
                price(0.25, "NORMAL", FUTURE_1),
                price(0.15, "CHEAP", FUTURE_2)), "null", "[]"));

        Optional<Price> result = service.getNextLowestPrice("test-token");

        assertThat(result).isPresent();
        assertThat(result.get().total()).isEqualTo(0.15);
        assertThat(result.get().startsAt()).isEqualTo(FUTURE_2);
    }

    @Test
    void getNextLowestPrice_withOnlyPastPrices_returnsEmpty() {
        server.respondWith(200, homeResponse("null", array(price(0.10, "CHEAP", PAST)), "null", "[]"));

        assertThat(service.getNextLowestPrice("test-token")).isEmpty();
    }

    @Test
    void getNextHighestPrice_ignoresPastPrices_returnsHighestFuturePrice() {
        server.respondWith(200, homeResponse("null", array(
                price(0.50, "VERY_EXPENSIVE", PAST),
                price(0.25, "NORMAL", FUTURE_1),
                price(0.40, "EXPENSIVE", FUTURE_2)), "null", "[]"));

        Optional<Price> result = service.getNextHighestPrice("test-token");

        assertThat(result).isPresent();
        assertThat(result.get().total()).isEqualTo(0.40);
    }

    @Test
    void getNextHighestPrice_withOnlyPastPrices_returnsEmpty() {
        server.respondWith(200, homeResponse("null", array(price(0.40, "EXPENSIVE", PAST)), "null", "[]"));

        assertThat(service.getNextHighestPrice("test-token")).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Fehlerfälle (Review Focus 1 + 2)
    // -------------------------------------------------------------------------

    @Test
    void query_withHttpError_throwsTibberApiException() {
        server.respondWith(500, "{}");

        assertThatThrownBy(() -> service.getCurrentPrice("test-token"))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("HTTP 500");
    }

    @Test
    void query_withGraphQlErrors_throwsWithMessageButWithoutToken() {
        server.respondWith(200, errorResponse("Context creation failed: invalid token"));

        assertThatThrownBy(() -> service.getCurrentPrice("secret-token-123"))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("invalid token")
                .message().doesNotContain("secret-token-123");
    }

    @Test
    void query_withoutHomes_throwsTibberApiException() {
        server.respondWith(200, """
                {"data":{"viewer":{"homes":[]}}}
                """);

        assertThatThrownBy(() -> service.getCurrentPrice("test-token"))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("Kein Zuhause");
    }

    @Test
    void query_withoutSubscription_throwsTibberApiException() {
        server.respondWith(200, """
                {"data":{"viewer":{"homes":[{"currentSubscription":null,"consumption":null}]}}}
                """);

        assertThatThrownBy(() -> service.getCurrentPrice("test-token"))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("Kein aktiver Tibber-Vertrag");
    }

    @Test
    void query_withInvalidJson_throwsTibberApiException() {
        server.respondWith(200, "not-json");

        assertThatThrownBy(() -> service.getCurrentPrice("test-token"))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("nicht lesbar");
    }

    @Test
    void query_whenServerTooSlow_throwsTibberApiException() {
        // KI-generiert: Request-Timeout im Test = 500 ms, Server antwortet erst nach 2 s
        server.respondWith(200, homeResponse(price(0.30, "EXPENSIVE", PAST), "[]", "null", "[]"));
        server.delayResponsesBy(Duration.ofSeconds(2));

        assertThatThrownBy(() -> service.getCurrentPrice("test-token"))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("nicht erreichbar");
    }
}
