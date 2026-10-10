package org.tholian.alexa.tibber.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.tholian.alexa.tibber.service.DatePeriod;
import org.tholian.alexa.tibber.service.EnergyKind;
import org.tholian.alexa.tibber.service.EnergyResult;
import org.tholian.alexa.tibber.service.CurrentUsage;
import org.tholian.alexa.tibber.service.Deadline;
import org.tholian.alexa.tibber.service.EnergyService;
import org.tholian.alexa.tibber.service.LiveMeasurementClient;
import org.tholian.alexa.tibber.service.LiveService;
import org.tholian.alexa.tibber.service.PriceSchedule;
import org.tholian.alexa.tibber.service.PriceService;
import org.tholian.alexa.tibber.service.TibberAccountService;
import org.tholian.alexa.tibber.service.TibberAuthenticationException;
import org.tholian.alexa.tibber.service.TibberGraphQlClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * KI-generiert: Integrationstest gegen die echte Tibber GraphQL API.
 * Läuft nur, wenn die Umgebungsvariable TIBBER_DEMO_TOKEN gesetzt ist (Demo-Token aus dem Tibber Developer Portal).
 *
 * Sicherheitshinweis: Der Token wird bewusst nicht eingecheckt und nirgends geloggt.
 * Datenabfluss: Der Test sendet den Token an api.tibber.com – nur manuell durch den User ausführen.
 */
@EnabledIfEnvironmentVariable(named = "TIBBER_DEMO_TOKEN", matches = ".+")
class TibberApiIntegrationTest {

    private static final URI TIBBER_API = URI.create("https://api.tibber.com/v1-beta/gql");
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private final String token = System.getenv("TIBBER_DEMO_TOKEN");
    private final TibberGraphQlClient client = new TibberGraphQlClient(HttpClient.newHttpClient(), new ObjectMapper(),
            TIBBER_API, Duration.ofSeconds(5));
    private final EnergyService energyService = new EnergyService(client, Clock.systemUTC());
    private final PriceService priceService = new PriceService(client);
    private final LiveService liveService = new LiveService(client,
            new LiveMeasurementClient(client.httpClient(), client.objectMapper()));
    private final TibberAccountService accountService = new TibberAccountService(client);

    @Test
    void prices_withDemoToken_returnsQuarterHourlyTodayWithParts() {
        PriceSchedule schedule = priceService.prices(token);

        // KI-generiert: Ein Tag hat 92–100 Viertelstunden (Zeitumstellung); Spotpreise können 0 oder negativ sein
        assertThat(schedule.day(LocalDate.now(BERLIN))).hasSizeBetween(92, 100)
                .allSatisfy(p -> assertThat(p.total()).isFinite());
        assertThat(schedule.current()).isNotNull();
        assertThat(schedule.current().energy()).as("energy").isNotNull();
        assertThat(schedule.current().tax()).as("tax").isNotNull();
    }

    @Test
    void currentUsage_withDemoToken_deliversLiveMeasurement() {
        // KI-generiert: Das Demo-Zuhause liefert Live-Daten. Ein Rückfall auf die letzte Stunde bedeutet: Subprotokoll,
        // User-Agent oder WebSocket-URL passen nicht zu Tibber – dann LiveMeasurementClient prüfen (Spec „Zu verifizieren“)
        CurrentUsage usage = liveService.currentUsage(token, Deadline.after(Duration.ofSeconds(10)));

        assertThat(usage).isInstanceOf(CurrentUsage.Live.class);
    }

    @Test
    void verifyToken_withDemoToken_passes() {
        accountService.verifyToken(token);
    }

    @Test
    void verifyToken_withInvalidToken_throwsAuthenticationException() {
        // KI-generiert: Pinnt das reale Ablehnungsverhalten der Tibber API (Statuscode bzw. GraphQL-Code)
        assertThatThrownBy(() -> accountService.verifyToken("invalid-token-for-integration-test"))
                .isInstanceOf(TibberAuthenticationException.class);
    }

    @Test
    void energy_withDemoToken_answersYesterdayLastMonthAndLastYear() {
        LocalDate today = LocalDate.now(BERLIN);
        for (String amazonDate : new String[] {today.minusDays(1).toString(),
                today.minusMonths(1).toString().substring(0, 7), String.valueOf(today.getYear() - 1)}) {
            EnergyResult result = energyService.total(token, DatePeriod.parse(amazonDate).orElseThrow(), EnergyKind.CONSUMPTION);

            // KI-generiert: Das Demo-Zuhause kann für einzelne Zeiträume keine Werte haben – beides ist gültig
            assertThat(result).as(amazonDate).isInstanceOfAny(EnergyResult.Total.class, EnergyResult.NoData.class);
        }
    }
}
