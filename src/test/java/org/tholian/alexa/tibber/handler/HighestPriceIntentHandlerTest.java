
package org.tholian.alexa.tibber.handler;

import com.amazon.ask.model.ui.LinkAccountCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.service.TibberService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// KI-generiert: Unit-Tests für HighestPriceIntentHandler — prüft canHandle, Token-Validierung, leere Preisdaten, Höchstpreis-Ausgabe und Fehlerfall
@ExtendWith(MockitoExtension.class)
class HighestPriceIntentHandlerTest {

    @Mock
    private TibberService tibberService;

    private HighestPriceIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new HighestPriceIntentHandler(tibberService);
    }

    @Test
    void canHandle_matchesGetHighestPriceIntent() {
        var input = HandlerTestHelper.intentInput("GetHighestPriceIntent", "token");
        assertThat(handler.canHandle(input)).isTrue();
    }

    @Test
    void canHandle_doesNotMatchOtherIntent() {
        var input = HandlerTestHelper.intentInput("GetLowestPriceIntent", "token");
        assertThat(handler.canHandle(input)).isFalse();
    }

    @Test
    void handle_withNullToken_returnsSpeechWithAccountLinkingHint() {
        var input = HandlerTestHelper.intentInput("GetHighestPriceIntent", null);

        var response = handler.handle(input);

        assertThat(response).isPresent();
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("verknüpfe");
        assertThat(response.get().getCard()).isInstanceOf(LinkAccountCard.class);
    }

    @Test
    void handle_withNoPriceData_returnsNoDataSpeech() {
        var input = HandlerTestHelper.intentInput("GetHighestPriceIntent", "test-token");
        when(tibberService.getNextHighestPrice("test-token")).thenReturn(Optional.empty());

        var response = handler.handle(input);

        assertThat(response).isPresent();
        // KI-generiert: Produktionscode liefert "Es sind keine Preisdaten für die nächsten Stunden verfügbar."
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("keine Preisdaten");
    }

    @Test
    void handle_withHighestPrice_returnsSpeechWithTimeAndPrice() {
        var input = HandlerTestHelper.intentInput("GetHighestPriceIntent", "test-token");
        var price = new Price(0.45, "EUR", "VERY_EXPENSIVE", "2026-04-22T18:00:00+02:00");
        when(tibberService.getNextHighestPrice("test-token")).thenReturn(Optional.of(price));
        when(tibberService.formatTime("2026-04-22T18:00:00+02:00")).thenReturn("18:00 Uhr");
        when(tibberService.formatPrice(0.45)).thenReturn("45,0 Cent");

        var response = handler.handle(input);

        assertThat(response).isPresent();
        String speech = HandlerTestHelper.extractSpeech(response.get());
        // KI-generiert: Produktionscode liefert "Der teuerste Strompreis in den nächsten 24 Stunden ist um ..."
        assertThat(speech).contains("18:00 Uhr").contains("45,0 Cent").contains("teuerste");
    }

    @Test
    void handle_whenServiceThrowsException_returnsErrorSpeech() {
        var input = HandlerTestHelper.intentInput("GetHighestPriceIntent", "test-token");
        when(tibberService.getNextHighestPrice("test-token"))
                .thenThrow(new RuntimeException("API unavailable"));

        var response = handler.handle(input);

        assertThat(response).isPresent();
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("Problem");
    }
}
