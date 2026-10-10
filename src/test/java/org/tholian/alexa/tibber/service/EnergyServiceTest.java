package org.tholian.alexa.tibber.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tholian.alexa.tibber.testsupport.FakeTibberServer;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.tholian.alexa.tibber.service.EnergyKind.CONSUMPTION;
import static org.tholian.alexa.tibber.service.EnergyKind.PRODUCTION;
import static org.tholian.alexa.tibber.testsupport.TibberJson.array;
import static org.tholian.alexa.tibber.testsupport.TibberJson.consumptionNode;
import static org.tholian.alexa.tibber.testsupport.TibberJson.consumptionResponse;
import static org.tholian.alexa.tibber.testsupport.TibberJson.productionNode;
import static org.tholian.alexa.tibber.testsupport.TibberJson.productionResponse;

/**
 * KI-generiert: Summen je Zeitraum gegen den FakeTibberServer. Uhr: Samstag, 10.10.2026, 10:00 Uhr Berlin.
 * Geprüft werden Auflösung und Anzahl (als GraphQL-Variablen) sowie die Zuordnung der Werte zum Zeitraum.
 */
class EnergyServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-10T08:00:00Z");

    private FakeTibberServer server;
    private TibberGraphQlClient client;
    private EnergyService service;

    @BeforeEach
    void setUp() {
        server = new FakeTibberServer();
        client = new TibberGraphQlClient(HttpClient.newHttpClient(), new ObjectMapper(), server.uri(), Duration.ofMillis(500));
        service = new EnergyService(client, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void total_today_sumsHourlyValuesSinceMidnight() {
        server.respondWith(200, consumptionResponse(array(
                consumptionNode("2026-10-09T23:00:00+02:00", 9.0, 9.0),
                consumptionNode("2026-10-10T00:00:00+02:00", 0.5, 0.15),
                consumptionNode("2026-10-10T01:00:00+02:00", 0.3, 0.06))));

        EnergyResult.Total total = total(service.total("test-token", period("2026-10-10"), CONSUMPTION));

        assertThat(server.lastRequestBody()).contains("\"resolution\":\"HOURLY\"").contains("\"last\":25");
        assertThat(total.energyKwh()).isCloseTo(0.8, within(1e-9));
        assertThat(total.amount()).isCloseTo(0.21, within(1e-9));
        assertThat(total.coverage()).isEqualTo(EnergyResult.Coverage.SO_FAR_TODAY);
    }

    @Test
    void total_yesterday_usesDailyValues() {
        server.respondWith(200, consumptionResponse(array(
                consumptionNode("2026-10-08T00:00:00+02:00", 7.0, 2.0),
                consumptionNode("2026-10-09T00:00:00+02:00", 8.4, 2.31))));

        EnergyResult.Total total = total(service.total("test-token", period("2026-10-09"), CONSUMPTION));

        assertThat(server.lastRequestBody()).contains("\"resolution\":\"DAILY\"").contains("\"last\":2");
        assertThat(total).isEqualTo(new EnergyResult.Total(8.4, 2.31, "EUR", EnergyResult.Coverage.COMPLETE));
    }

    @Test
    void total_currentWeek_sumsCompletedDaysUntilYesterday() {
        server.respondWith(200, consumptionResponse(array(
                consumptionNode("2026-10-04T00:00:00+02:00", 50.0, 9.0),
                consumptionNode("2026-10-05T00:00:00+02:00", 1.0, 0.3),
                consumptionNode("2026-10-09T00:00:00+02:00", 2.0, 0.6),
                consumptionNode("2026-10-10T00:00:00+02:00", 5.0, 1.5))));

        EnergyResult.Total total = total(service.total("test-token", period("2026-W41"), CONSUMPTION));

        assertThat(server.lastRequestBody()).contains("\"resolution\":\"DAILY\"").contains("\"last\":6");
        assertThat(total.energyKwh()).isCloseTo(3.0, within(1e-9));
        assertThat(total.coverage()).isEqualTo(EnergyResult.Coverage.UNTIL_YESTERDAY);
    }

    @Test
    void total_currentWeekOnMonday_usesTodayHourly() {
        var monday = new EnergyService(client, Clock.fixed(Instant.parse("2026-10-12T08:00:00Z"), ZoneOffset.UTC));
        server.respondWith(200, consumptionResponse(array(consumptionNode("2026-10-12T00:00:00+02:00", 0.4, 0.1))));

        EnergyResult.Total total = total(monday.total("test-token", period("2026-W42"), CONSUMPTION));

        assertThat(server.lastRequestBody()).contains("\"resolution\":\"HOURLY\"");
        assertThat(total.coverage()).isEqualTo(EnergyResult.Coverage.SO_FAR_TODAY);
    }

    @Test
    void total_pastMonthWithin400Days_usesDailyValues() {
        server.respondWith(200, consumptionResponse(array(
                consumptionNode("2026-09-01T00:00:00+02:00", 10.0, 3.0),
                consumptionNode("2026-09-30T00:00:00+02:00", 5.0, 1.5),
                consumptionNode("2026-10-01T00:00:00+02:00", 99.0, 30.0))));

        EnergyResult.Total total = total(service.total("test-token", period("2026-09"), CONSUMPTION));

        assertThat(server.lastRequestBody()).contains("\"resolution\":\"DAILY\"").contains("\"last\":40");
        assertThat(total.energyKwh()).isCloseTo(15.0, within(1e-9));
        assertThat(total.coverage()).isEqualTo(EnergyResult.Coverage.COMPLETE);
    }

    @Test
    void total_olderMonth_usesMonthlyValues() {
        server.respondWith(200, consumptionResponse(array(consumptionNode("2024-01-01T00:00:00+01:00", 300.0, 90.0))));

        EnergyResult.Total total = total(service.total("test-token", period("2024-01"), CONSUMPTION));

        assertThat(server.lastRequestBody()).contains("\"resolution\":\"MONTHLY\"").contains("\"last\":34");
        assertThat(total.energyKwh()).isCloseTo(300.0, within(1e-9));
    }

    @Test
    void total_pastYear_usesAnnualValues() {
        server.respondWith(200, consumptionResponse(array(
                consumptionNode("2025-01-01T00:00:00+01:00", 3000.0, 900.0),
                consumptionNode("2026-01-01T00:00:00+01:00", 2000.0, 600.0))));

        EnergyResult.Total total = total(service.total("test-token", period("2025"), CONSUMPTION));

        assertThat(server.lastRequestBody()).contains("\"resolution\":\"ANNUAL\"").contains("\"last\":2");
        assertThat(total.energyKwh()).isCloseTo(3000.0, within(1e-9));
    }

    @Test
    void total_currentYear_combinesCompletedMonthsAndDaysOfThisMonth() {
        server.respondTo("\"resolution\":\"MONTHLY\"", consumptionResponse(array(
                consumptionNode("2026-09-01T00:00:00+02:00", 100.0, 30.0))));
        server.respondTo("\"resolution\":\"DAILY\"", consumptionResponse(array(
                consumptionNode("2026-10-01T00:00:00+02:00", 5.0, 1.5))));

        EnergyResult.Total total = total(service.total("test-token", period("2026"), CONSUMPTION));

        assertThat(server.requestBodies()).hasSize(2).allSatisfy(body -> assertThat(body).contains("\"last\":10"));
        assertThat(total.energyKwh()).isCloseTo(105.0, within(1e-9));
        assertThat(total.amount()).isCloseTo(31.5, within(1e-9));
        assertThat(total.coverage()).isEqualTo(EnergyResult.Coverage.UNTIL_YESTERDAY);
    }

    @Test
    void total_tooOld_returnsTooOldWithoutRequest() {
        assertThat(service.total("test-token", period("2000"), CONSUMPTION)).isInstanceOf(EnergyResult.TooOld.class);
        assertThat(service.total("test-token", period("2024-01-15"), CONSUMPTION)).isInstanceOf(EnergyResult.TooOld.class);
        assertThat(server.requestCount()).isZero();
    }

    @Test
    void total_future_returnsFutureWithoutRequest() {
        assertThat(service.total("test-token", period("2026-10-11"), CONSUMPTION)).isInstanceOf(EnergyResult.Future.class);
        assertThat(server.requestCount()).isZero();
    }

    @Test
    void total_withoutMeasuredValues_returnsNoData() {
        server.respondWith(200, consumptionResponse(array(consumptionNode("2026-10-09T00:00:00+02:00", null, null))));

        assertThat(service.total("test-token", period("2026-10-09"), CONSUMPTION)).isInstanceOf(EnergyResult.NoData.class);
    }

    @Test
    void total_withoutConsumptionBlock_returnsNoData() {
        server.respondWith(200, "{\"data\":{\"viewer\":{\"homes\":[{\"consumption\":null}]}}}");

        assertThat(service.total("test-token", period("2026-10-09"), CONSUMPTION)).isInstanceOf(EnergyResult.NoData.class);
    }

    @Test
    void total_withMissingCost_omitsAmount() {
        server.respondWith(200, consumptionResponse(array(
                consumptionNode("2026-10-05T00:00:00+02:00", 1.0, 0.3),
                consumptionNode("2026-10-06T00:00:00+02:00", 2.0, null))));

        EnergyResult.Total total = total(service.total("test-token", period("2026-W41"), CONSUMPTION));

        assertThat(total.energyKwh()).isCloseTo(3.0, within(1e-9));
        assertThat(total.amount()).isNull();
    }

    @Test
    void total_production_queriesProductionFields() {
        server.respondWith(200, productionResponse(array(productionNode("2026-10-09T00:00:00+02:00", 12.5, 1.1))));

        EnergyResult.Total total = total(service.total("test-token", period("2026-10-09"), PRODUCTION));

        assertThat(server.lastRequestBody()).contains("production(").contains("profit").doesNotContain("consumption(");
        assertThat(total).isEqualTo(new EnergyResult.Total(12.5, 1.1, "EUR", EnergyResult.Coverage.COMPLETE));
    }

    @Test
    void total_currentYear_sharesOneTimeBudget() {
        // KI-generiert: Zwei Abfragen à 300 ms überschreiten das gemeinsame Budget von 500 ms → Fehler statt ~600 ms Erfolg
        server.respondTo("\"resolution\":\"MONTHLY\"", consumptionResponse(array(
                consumptionNode("2026-09-01T00:00:00+02:00", 100.0, 30.0))));
        server.respondTo("\"resolution\":\"DAILY\"", consumptionResponse(array(
                consumptionNode("2026-10-01T00:00:00+02:00", 5.0, 1.5))));
        server.delayResponsesBy(Duration.ofMillis(300));
        long start = System.nanoTime();

        assertThatThrownBy(() -> service.total("test-token", period("2026"), CONSUMPTION))
                .isInstanceOf(TibberApiException.class);

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(900));
    }

    @Test
    void total_singleRequestWithinBudget_succeedsDespiteDelay() {
        server.respondWith(200, consumptionResponse(array(consumptionNode("2026-10-09T00:00:00+02:00", 8.4, 2.31))));
        server.delayResponsesBy(Duration.ofMillis(300));

        assertThat(service.total("test-token", period("2026-10-09"), CONSUMPTION)).isInstanceOf(EnergyResult.Total.class);
    }

    @Test
    void total_unreadableTimestamp_throwsApiExceptionWithoutValue() {
        server.respondWith(200, consumptionResponse(array(consumptionNode("kaputt", 1.0, 0.3))));

        assertThatThrownBy(() -> service.total("test-token", period("2026-10-09"), CONSUMPTION))
                .isInstanceOf(TibberApiException.class)
                .hasMessage("Tibber-Antwort unlesbar");
    }

    private static DatePeriod period(String amazonDate) {
        return DatePeriod.parse(amazonDate).orElseThrow();
    }

    private static EnergyResult.Total total(EnergyResult result) {
        assertThat(result).isInstanceOf(EnergyResult.Total.class);
        return (EnergyResult.Total) result;
    }
}
