package org.tholian.alexa.tibber.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.tholian.alexa.tibber.model.ConsumptionNode;
import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.service.TibberService;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KI-generiert: Integrationstest gegen die echte Tibber GraphQL API.
 * Läuft nur, wenn die Umgebungsvariable TIBBER_DEMO_TOKEN gesetzt ist (öffentlicher Demo-Token
 * aus der Tibber-Developer-Dokumentation). Sonst wird er übersprungen.
 *
 * Sicherheitshinweis: Der Token wird bewusst nicht eingecheckt und nirgends geloggt.
 * Datenabfluss: Der Test sendet den Token an api.tibber.com – nur manuell ausführen.
 */
@EnabledIfEnvironmentVariable(named = "TIBBER_DEMO_TOKEN", matches = ".+")
class TibberServiceIntegrationTest {

    private static final URI TIBBER_API = URI.create("https://api.tibber.com/v1-beta/gql");

    private final String token = System.getenv("TIBBER_DEMO_TOKEN");
    private final TibberService tibberService = new TibberService(HttpClient.newHttpClient(), new ObjectMapper(),
            TIBBER_API, Duration.ofSeconds(5), Clock.systemUTC());

    @Test
    void getCurrentPrice_withDemoToken_returnsValidPrice() {
        Price price = tibberService.getCurrentPrice(token);

        // KI-generiert: isFinite() statt isGreaterThan(0.0) – Spotpreise können 0 oder negativ sein
        assertThat(price.total()).isFinite();
        assertThat(price.startsAt()).isNotBlank();
    }

    @Test
    void getPriceList_withDemoToken_returnsNonEmptyList() {
        List<Price> prices = tibberService.getPriceList(token);

        assertThat(prices).isNotEmpty().allSatisfy(p -> {
            assertThat(p.total()).isFinite();
            assertThat(p.startsAt()).isNotBlank();
        });
    }

    @Test
    void getCurrentConsumption_withDemoToken_returnsOptional() {
        Optional<ConsumptionNode> result = tibberService.getCurrentConsumption(token);

        // KI-generiert: Ergebnis kann leer sein (keine Verbrauchsdaten in der letzten Stunde) – das ist valide
        result.ifPresent(node -> {
            assertThat(node.from()).isNotBlank();
            assertThat(node.to()).isNotBlank();
        });
    }
}
