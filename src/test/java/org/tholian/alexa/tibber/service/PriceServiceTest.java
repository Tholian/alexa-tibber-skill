package org.tholian.alexa.tibber.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.testsupport.FakeTibberServer;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.tholian.alexa.tibber.testsupport.TibberJson.array;
import static org.tholian.alexa.tibber.testsupport.TibberJson.price;
import static org.tholian.alexa.tibber.testsupport.TibberJson.priceInfoResponse;

/** KI-generiert: Preisabfrage in Viertelstunden-Auflösung gegen den FakeTibberServer. */
class PriceServiceTest {

    private FakeTibberServer server;
    private PriceService service;

    @BeforeEach
    void setUp() {
        server = new FakeTibberServer();
        service = new PriceService(new TibberGraphQlClient(HttpClient.newHttpClient(), new ObjectMapper(), server.uri(),
                Duration.ofMillis(500)));
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void prices_requestsQuarterHourlyPricesWithParts() {
        server.respondWith(200, priceInfoResponse("null", "[]", "[]"));

        service.prices("test-token");

        assertThat(server.lastAuthorization()).isEqualTo("Bearer test-token");
        // KI-generiert: Datensparsamkeit – die Preisabfrage liest keine Verbrauchsdaten
        assertThat(server.lastRequestBody()).contains("QUARTER_HOURLY").contains("energy").contains("tax")
                .doesNotContain("consumption");
    }

    @Test
    void prices_combinesTodayAndTomorrowSorted() {
        server.respondWith(200, priceInfoResponse(price(0.2, "NORMAL", "2026-10-10T10:15:00+02:00"),
                array(price(0.2, "NORMAL", "2026-10-10T10:15:00+02:00"), price(0.1, "CHEAP", "2026-10-10T10:00:00+02:00")),
                array(price(0.3, "EXPENSIVE", "2026-10-11T00:00:00+02:00"))));

        PriceSchedule schedule = service.prices("test-token");

        assertThat(schedule.current()).isNotNull();
        assertThat(schedule.current().total()).isEqualTo(0.2);
        assertThat(schedule.slots()).extracting(Price::total).containsExactly(0.1, 0.2, 0.3);
        assertThat(schedule.currency()).isEqualTo("EUR");
    }

    @Test
    void prices_readsEnergyAndTax() {
        server.respondWith(200, priceInfoResponse(price(0.234, 0.121, 0.113, "NORMAL", "2026-10-10T10:00:00+02:00"),
                "[]", "null"));

        Price current = service.prices("test-token").current();

        assertThat(current).isNotNull();
        assertThat(current.energy()).isEqualTo(0.121);
        assertThat(current.tax()).isEqualTo(0.113);
    }

    @Test
    void prices_withNullTomorrow_returnsToday() {
        server.respondWith(200, priceInfoResponse("null", array(price(0.2, "NORMAL", "2026-10-10T10:00:00+02:00")), "null"));

        assertThat(service.prices("test-token").slots()).hasSize(1);
    }

    @Test
    void prices_withoutSubscription_throws() {
        server.respondWith(200, "{\"data\":{\"viewer\":{\"homes\":[{\"currentSubscription\":null}]}}}");

        assertThatThrownBy(() -> service.prices("test-token"))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("Kein aktiver Tibber-Vertrag");
    }

    @Test
    void prices_withoutPriceInfo_throws() {
        server.respondWith(200, "{\"data\":{\"viewer\":{\"homes\":[{\"currentSubscription\":{\"priceInfo\":null}}]}}}");

        assertThatThrownBy(() -> service.prices("test-token"))
                .isInstanceOf(TibberApiException.class)
                .hasMessageContaining("keine Preisinformationen");
    }
}
