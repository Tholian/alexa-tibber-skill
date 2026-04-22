
package org.tholian.alexa.tibber.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.tholian.alexa.tibber.model.ConsumptionNode;
import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.service.TibberService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// KI-generiert: Integrationstest gegen die echte Tibber GraphQL API.
// Sicherheitshinweis — Demo-Token ist ein öffentlicher Test-Token von Tibber,
// kein Produktions-Secret. Nur für lokale Entwicklung verwenden.
// Der Test ist mit @Disabled deaktiviert und muss manuell aktiviert werden.
@SpringBootTest
@Disabled("Nur manuell ausführen: erfordert Netzwerkzugang zur Tibber API")
class TibberServiceIntegrationTest {

    // KI-generiert: Öffentlicher Tibber Demo-Token (Stand April 2026, kein Produktions-Secret).
    // Bei UNAUTHENTICATED-Fehler: Token abgelaufen, neuen in Tibber API-Dokumentation suchen.
    private static final String DEMO_TOKEN = "3A77EECF61BD445F47241A5A36202185C35AF3AF58609E19B53F3A8872AD7BE1-1";

    @Autowired
    private TibberService tibberService;

    /**
     * KI-generiert: Prüft Struktur der getCurrentPrice()-Antwort.
     * Wertet nur strukturelle Validität aus — kein Preiswert-Check,
     * da Spotmarkt-Preise auch 0.0 oder negativ sein können.
     */
    @Test
    void getCurrentPrice_withDemoToken_returnsValidPrice() {
        Price price = tibberService.getCurrentPrice(DEMO_TOKEN);

        assertThat(price).isNotNull();
        // KI-generiert: isFinite() statt isGreaterThan(0.0) — Spotpreise können 0 oder negativ sein
        assertThat(price.total()).isFinite();
        assertThat(price.startsAt()).isNotBlank();
        // currency und level können im Demo-Account leer sein; isNotNull() reicht strukturell
        assertThat(price.currency()).isNotNull();
        assertThat(price.level()).isNotNull();
    }

    /**
     * KI-generiert: Prüft, dass getPriceList() eine nicht-leere Liste zurückgibt.
     * Preise für heute sind im Tibber-System immer vorhanden;
     * morgen kann null sein (Preise werden ab ~13 Uhr veröffentlicht).
     */
    @Test
    void getPriceList_withDemoToken_returnsNonEmptyList() {
        List<Price> prices = tibberService.getPriceList(DEMO_TOKEN);

        assertThat(prices).isNotEmpty();
        prices.forEach(p -> {
            assertThat(p.total()).isFinite();
            assertThat(p.startsAt()).isNotBlank();
        });
    }

    /**
     * KI-generiert: Prüft getCurrentConsumption() — Optional kann leer sein,
     * wenn keine Verbrauchsdaten für die letzte Stunde vorhanden sind.
     * Wenn ein Knoten vorhanden, wird die Struktur geprüft.
     */
    @Test
    void getCurrentConsumption_withDemoToken_returnsOptional() {
        Optional<ConsumptionNode> result = tibberService.getCurrentConsumption(DEMO_TOKEN);

        // KI-generiert: Ergebnis kann leer sein (kein Verbrauch in letzter Stunde) — das ist valide
        assertThat(result).isNotNull();
        result.ifPresent(node -> {
            assertThat(node.from()).isNotBlank();
            assertThat(node.to()).isNotBlank();
        });
    }
}
