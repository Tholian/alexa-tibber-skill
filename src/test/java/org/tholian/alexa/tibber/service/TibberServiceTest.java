
package org.tholian.alexa.tibber.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;
import org.tholian.alexa.tibber.model.ConsumptionNode;
import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.model.TibberResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * KI-generiert: Unit-Tests für TibberService – Tasks 1, 2 und 3.
 * Testet Formatierungsmethoden, API-Methoden und Filter-Methoden.
 *
 * Sicherheitshinweis: Der echte RestClient wird durch einen Mock ersetzt –
 * es findet kein Netzwerkzugriff und kein Umgang mit echten Tokens statt.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TibberServiceTest {

    @Mock
    private RestClient restClient;

    // KI-generiert: RequestBodySpec statt RequestBodyUriSpec verwenden, da
    // header() auf RequestBodyUriSpec die Rückgabe von RequestBodySpec hat –
    // damit wird die gesamte Kette über einen einzigen Mock abgebildet.
    @Mock
    private RestClient.RequestBodySpec bodySpec;

    @Mock
    private RestClient.RequestBodyUriSpec postSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private TibberService service;

    @BeforeEach
    void setUp() {
        // KI-generiert: Fluent-Chain manuell aufbauen:
        //   restClient.post()             → postSpec  (RequestBodyUriSpec)
        //   postSpec.header(*, *)         → bodySpec  (RequestBodySpec – Rückgabetyp von header())
        //   bodySpec.body(*)              → bodySpec  (RequestBodySpec – Rückgabetyp von body())
        //   bodySpec.retrieve()           → responseSpec
        //   responseSpec.body(Class)      → wird pro Test gesetzt
        //
        // lenient() verhindert UnnecessaryStubbingException in reinen Format-Tests.
        lenient().when(restClient.post()).thenReturn(postSpec);
        // KI-generiert: anyString() für den Header-Namen, any(String[].class) für den Varargs-Wert
        lenient().when(postSpec.header(anyString(), any(String[].class))).thenReturn(bodySpec);
        lenient().when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        lenient().when(bodySpec.retrieve()).thenReturn(responseSpec);
        service = new TibberService(restClient);
    }

    /**
     * KI-generiert: Hilfsmethode zum Aufbau einer vollständigen TibberResponse
     * aus den einzelnen Bestandteilen (current price, today, tomorrow, consumption nodes).
     */
    private TibberResponse buildResponse(Price current, List<Price> today, List<Price> tomorrow,
                                          List<ConsumptionNode> nodes) {
        var priceInfo = new TibberResponse.PriceInfo(current, today, tomorrow);
        var subscription = new TibberResponse.CurrentSubscription(priceInfo);
        var consumption = new TibberResponse.Consumption(nodes);
        var home = new TibberResponse.Home(subscription, consumption);
        var viewer = new TibberResponse.Viewer(List.of(home));
        var data = new TibberResponse.Data(viewer);
        return new TibberResponse(data);
    }

    // -------------------------------------------------------------------------
    // Task 1: Formatierungsmethoden
    // -------------------------------------------------------------------------

    /**
     * KI-generiert: Prüft, dass Euro-Betrag korrekt in Cent umgerechnet und formatiert wird.
     */
    @Test
    void formatPrice_convertsEuroToCent() {
        assertThat(service.formatPrice(0.25)).isEqualTo("25,0 Cent");
    }

    /**
     * KI-generiert: Prüft Randfall – 0 Euro ergibt 0,0 Cent.
     */
    @Test
    void formatPrice_zeroEuro_returnsZeroCent() {
        assertThat(service.formatPrice(0.0)).isEqualTo("0,0 Cent");
    }

    /**
     * KI-generiert: 2026-06-15 liegt sicher in CEST (UTC+2), UTC 10:00 = Berlin 12:00.
     * Testet Sommerzeit-Konvertierung, DST-unabhängig vom Ausführungsdatum.
     */
    @Test
    void formatTime_convertsIsoStringToBerlinTimeSummer() {
        // KI-generiert: 2026-06-15 liegt sicher in CEST (UTC+2), UTC 10:00 = Berlin 12:00
        String isoUtc = "2026-06-15T10:00:00Z";
        assertThat(service.formatTime(isoUtc)).isEqualTo("12:00 Uhr");
    }

    /**
     * KI-generiert: 2026-01-15 liegt sicher in CET (UTC+1), UTC 11:00 = Berlin 12:00.
     * Testet Winterzeit-Konvertierung, DST-unabhängig vom Ausführungsdatum.
     */
    @Test
    void formatTime_convertsIsoStringToBerlinTimeWinter() {
        // KI-generiert: 2026-01-15 liegt sicher in CET (UTC+1), UTC 11:00 = Berlin 12:00
        String isoUtc = "2026-01-15T11:00:00Z";
        assertThat(service.formatTime(isoUtc)).isEqualTo("12:00 Uhr");
    }

    /** KI-generiert: Prüft Label für Preisstufe VERY_CHEAP. */
    @Test
    void formatLevel_veryCheap_returnsGermanLabel() {
        assertThat(service.formatLevel("VERY_CHEAP")).isEqualTo("sehr günstig");
    }

    /** KI-generiert: Prüft Label für Preisstufe CHEAP. */
    @Test
    void formatLevel_cheap_returnsGermanLabel() {
        assertThat(service.formatLevel("CHEAP")).isEqualTo("günstig");
    }

    /** KI-generiert: Prüft Label für Preisstufe NORMAL. */
    @Test
    void formatLevel_normal_returnsGermanLabel() {
        assertThat(service.formatLevel("NORMAL")).isEqualTo("normal");
    }

    /** KI-generiert: Prüft Label für Preisstufe EXPENSIVE. */
    @Test
    void formatLevel_expensive_returnsGermanLabel() {
        assertThat(service.formatLevel("EXPENSIVE")).isEqualTo("teuer");
    }

    /** KI-generiert: Prüft Label für Preisstufe VERY_EXPENSIVE. */
    @Test
    void formatLevel_veryExpensive_returnsGermanLabel() {
        assertThat(service.formatLevel("VERY_EXPENSIVE")).isEqualTo("sehr teuer");
    }

    /** KI-generiert: Prüft Fallback – unbekannte Level-Strings werden lowercase zurückgegeben. */
    @Test
    void formatLevel_unknown_returnsLowercase() {
        assertThat(service.formatLevel("UNKNOWN_LEVEL")).isEqualTo("unknown_level");
    }

    // -------------------------------------------------------------------------
    // Task 2: API-Methoden
    // -------------------------------------------------------------------------

    /**
     * KI-generiert: Prüft, dass getCurrentPrice den aktuellen Preis aus der API-Antwort zurückgibt.
     */
    @Test
    void getCurrentPrice_returnsCurrentPriceFromResponse() {
        Price current = new Price(0.30, "EUR", "EXPENSIVE", "2026-04-22T14:00:00+02:00");
        when(responseSpec.body(TibberResponse.class))
                .thenReturn(buildResponse(current, List.of(), null, List.of()));

        Price result = service.getCurrentPrice("test-token");

        assertThat(result.total()).isEqualTo(0.30);
        assertThat(result.level()).isEqualTo("EXPENSIVE");
    }

    /**
     * KI-generiert: Prüft, dass getPriceList heute und morgen kombiniert zurückgibt.
     */
    @Test
    void getPriceList_combinesTodayAndTomorrow() {
        Price today1 = new Price(0.20, "EUR", "CHEAP", "2026-04-22T10:00:00+02:00");
        Price tomorrow1 = new Price(0.35, "EUR", "EXPENSIVE", "2026-04-23T10:00:00+02:00");
        when(responseSpec.body(TibberResponse.class))
                .thenReturn(buildResponse(null, List.of(today1), List.of(tomorrow1), List.of()));

        List<Price> result = service.getPriceList("test-token");

        // KI-generiert: positionsunabhängige Assertion – Reihenfolge der Einträge ist nicht relevant
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Price::startsAt)
                .anySatisfy(s -> assertThat(s).contains("2026-04-22"))
                .anySatisfy(s -> assertThat(s).contains("2026-04-23"));
    }

    /**
     * KI-generiert: Prüft Randfall – wenn tomorrow null ist, enthält die Liste nur heute-Preise.
     */
    @Test
    void getPriceList_withNullTomorrow_returnsOnlyToday() {
        Price today1 = new Price(0.20, "EUR", "CHEAP", "2026-04-22T10:00:00+02:00");
        when(responseSpec.body(TibberResponse.class))
                .thenReturn(buildResponse(null, List.of(today1), null, List.of()));

        List<Price> result = service.getPriceList("test-token");

        assertThat(result).hasSize(1);
    }

    /**
     * KI-generiert: Prüft, dass getCurrentConsumption einen Verbrauchsknoten zurückgibt wenn Daten vorhanden sind.
     */
    @Test
    void getCurrentConsumption_withNode_returnsOptionalOfNode() {
        ConsumptionNode node = new ConsumptionNode(
                "2026-04-22T13:00:00+02:00", "2026-04-22T14:00:00+02:00",
                0.5, "kWh", 0.125);
        when(responseSpec.body(TibberResponse.class))
                .thenReturn(buildResponse(null, List.of(), null, List.of(node)));

        Optional<ConsumptionNode> result = service.getCurrentConsumption("test-token");

        assertThat(result).isPresent();
        assertThat(result.get().consumption()).isEqualTo(0.5);
    }

    /**
     * KI-generiert: Prüft Randfall – wenn keine Verbrauchsknoten vorhanden, wird Optional.empty() zurückgegeben.
     */
    @Test
    void getCurrentConsumption_withEmptyNodes_returnsEmpty() {
        when(responseSpec.body(TibberResponse.class))
                .thenReturn(buildResponse(null, List.of(), null, List.of()));

        Optional<ConsumptionNode> result = service.getCurrentConsumption("test-token");

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Task 3: Filter-Methoden
    // -------------------------------------------------------------------------

    /**
     * KI-generiert: Prüft, dass getNextLowestPrice vergangene Preise ignoriert
     * und den günstigsten zukünftigen Preis zurückgibt.
     */
    @Test
    void getNextLowestPrice_ignoresPastPrices_returnsLowestFuturePrice() {
        String past = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1).toString();
        String future1 = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1).toString();
        String future2 = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2).toString();

        List<Price> today = List.of(
                new Price(0.10, "EUR", "VERY_CHEAP", past),
                new Price(0.25, "EUR", "NORMAL", future1),
                new Price(0.15, "EUR", "CHEAP", future2)
        );
        when(responseSpec.body(TibberResponse.class))
                .thenReturn(buildResponse(null, today, null, List.of()));

        Optional<Price> result = service.getNextLowestPrice("test-token");

        assertThat(result).isPresent();
        assertThat(result.get().total()).isEqualTo(0.15);
        assertThat(result.get().startsAt()).isEqualTo(future2);
    }

    /**
     * KI-generiert: Prüft Randfall – wenn nur vergangene Preise vorhanden sind, wird Optional.empty() zurückgegeben.
     */
    @Test
    void getNextLowestPrice_withOnlyPastPrices_returnsEmpty() {
        String past = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2).toString();
        List<Price> today = List.of(new Price(0.10, "EUR", "CHEAP", past));
        when(responseSpec.body(TibberResponse.class))
                .thenReturn(buildResponse(null, today, null, List.of()));

        Optional<Price> result = service.getNextLowestPrice("test-token");

        assertThat(result).isEmpty();
    }

    /**
     * KI-generiert: Prüft, dass getNextHighestPrice vergangene Preise ignoriert
     * und den teuersten zukünftigen Preis zurückgibt.
     */
    @Test
    void getNextHighestPrice_ignoresPastPrices_returnsHighestFuturePrice() {
        String past = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1).toString();
        String future1 = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1).toString();
        String future2 = OffsetDateTime.now(ZoneOffset.UTC).plusHours(2).toString();

        List<Price> today = List.of(
                new Price(0.50, "EUR", "VERY_EXPENSIVE", past),
                new Price(0.25, "EUR", "NORMAL", future1),
                new Price(0.40, "EUR", "EXPENSIVE", future2)
        );
        when(responseSpec.body(TibberResponse.class))
                .thenReturn(buildResponse(null, today, null, List.of()));

        Optional<Price> result = service.getNextHighestPrice("test-token");

        assertThat(result).isPresent();
        assertThat(result.get().total()).isEqualTo(0.40);
    }

    /**
     * KI-generiert: Prüft Randfall – wenn nur vergangene Preise vorhanden sind, wird Optional.empty() zurückgegeben.
     */
    @Test
    void getNextHighestPrice_withOnlyPastPrices_returnsEmpty() {
        String past = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2).toString();
        List<Price> today = List.of(new Price(0.40, "EUR", "EXPENSIVE", past));
        when(responseSpec.body(TibberResponse.class))
                .thenReturn(buildResponse(null, today, null, List.of()));

        Optional<Price> result = service.getNextHighestPrice("test-token");

        assertThat(result).isEmpty();
    }
}
