
package org.tholian.alexa.tibber.handler;

import com.amazon.ask.model.ui.LinkAccountCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tholian.alexa.tibber.model.ConsumptionNode;
import org.tholian.alexa.tibber.service.TibberService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// KI-generiert: Unit-Tests für CurrentConsumptionIntentHandler — prüft canHandle, Token-Validierung, leere Daten, Verbrauchsdaten und Fehlerfall
@ExtendWith(MockitoExtension.class)
class CurrentConsumptionIntentHandlerTest {

    @Mock
    private TibberService tibberService;

    private CurrentConsumptionIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CurrentConsumptionIntentHandler(tibberService);
    }

    @Test
    void canHandle_matchesGetCurrentConsumptionIntent() {
        var input = HandlerTestHelper.intentInput("GetCurrentConsumptionIntent", "token");
        assertThat(handler.canHandle(input)).isTrue();
    }

    @Test
    void canHandle_doesNotMatchOtherIntent() {
        var input = HandlerTestHelper.intentInput("GetCurrentPriceIntent", "token");
        assertThat(handler.canHandle(input)).isFalse();
    }

    @Test
    void handle_withNullToken_returnsSpeechWithAccountLinkingHint() {
        var input = HandlerTestHelper.intentInput("GetCurrentConsumptionIntent", null);

        var response = handler.handle(input);

        assertThat(response).isPresent();
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("verknüpfe");
        assertThat(response.get().getCard()).isInstanceOf(LinkAccountCard.class);
    }

    @Test
    void handle_withNoConsumptionData_returnsNoDataSpeech() {
        var input = HandlerTestHelper.intentInput("GetCurrentConsumptionIntent", "test-token");
        when(tibberService.getCurrentConsumption("test-token")).thenReturn(Optional.empty());

        var response = handler.handle(input);

        assertThat(response).isPresent();
        // KI-generiert: Produktionscode liefert "Es sind noch keine Verbrauchsdaten verfügbar."
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("keine Verbrauchsdaten");
    }

    @Test
    void handle_withConsumptionData_returnsSpeechWithKwhAndCost() {
        var input = HandlerTestHelper.intentInput("GetCurrentConsumptionIntent", "test-token");
        var node = new ConsumptionNode(
                "2026-04-22T13:00:00+02:00", "2026-04-22T14:00:00+02:00",
                0.5, "kWh", 0.125);
        when(tibberService.getCurrentConsumption("test-token")).thenReturn(Optional.of(node));
        when(tibberService.formatTime("2026-04-22T13:00:00+02:00")).thenReturn("13:00 Uhr");

        var response = handler.handle(input);

        assertThat(response).isPresent();
        String speech = HandlerTestHelper.extractSpeech(response.get());
        // KI-generiert: Produktionscode nutzt Locale.GERMAN mit %.2f → "0,50" und "0,13"
        assertThat(speech).contains("13:00 Uhr").contains("0,50").contains("0,13");
    }

    @Test
    void handle_withPartialConsumptionData_returnsIncompleteDataSpeech() {
        var input = HandlerTestHelper.intentInput("GetCurrentConsumptionIntent", "test-token");
        // KI-generiert: Testfall für ConsumptionNode mit null-Werten (Daten noch nicht vollständig)
        var node = new ConsumptionNode(
                "2026-04-22T13:00:00+02:00", "2026-04-22T14:00:00+02:00",
                null, "kWh", null);
        when(tibberService.getCurrentConsumption("test-token")).thenReturn(Optional.of(node));
        when(tibberService.formatTime("2026-04-22T13:00:00+02:00")).thenReturn("13:00 Uhr");

        var response = handler.handle(input);

        assertThat(response).isPresent();
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("vollständigen Verbrauchsdaten");
    }

    @Test
    void handle_whenServiceThrowsException_returnsErrorSpeech() {
        var input = HandlerTestHelper.intentInput("GetCurrentConsumptionIntent", "test-token");
        when(tibberService.getCurrentConsumption("test-token"))
                .thenThrow(new RuntimeException("API unavailable"));

        var response = handler.handle(input);

        assertThat(response).isPresent();
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("Problem");
    }
}
