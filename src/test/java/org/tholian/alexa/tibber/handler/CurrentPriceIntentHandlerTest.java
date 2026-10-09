
package org.tholian.alexa.tibber.handler;

import com.amazon.ask.model.ui.LinkAccountCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.service.TibberAuthenticationException;
import org.tholian.alexa.tibber.service.TibberService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// KI-generiert: Unit-Tests für CurrentPriceIntentHandler — prüft canHandle, Token-Validierung, Erfolgsfall und Fehlerfall
@ExtendWith(MockitoExtension.class)
class CurrentPriceIntentHandlerTest {

    @Mock
    private TibberService tibberService;

    private CurrentPriceIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CurrentPriceIntentHandler(tibberService);
    }

    @Test
    void canHandle_matchesGetCurrentPriceIntent() {
        var input = HandlerTestHelper.intentInput("GetCurrentPriceIntent", "token");
        assertThat(handler.canHandle(input)).isTrue();
    }

    @Test
    void canHandle_doesNotMatchOtherIntent() {
        var input = HandlerTestHelper.intentInput("AMAZON.HelpIntent", "token");
        assertThat(handler.canHandle(input)).isFalse();
    }

    @Test
    void handle_withNullToken_returnsSpeechWithAccountLinkingHint() {
        var input = HandlerTestHelper.intentInput("GetCurrentPriceIntent", null);

        var response = handler.handle(input);

        assertThat(response).isPresent();
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("verknüpfe");
        assertThat(response.get().getCard()).isInstanceOf(LinkAccountCard.class);
    }

    @Test
    void handle_withValidToken_returnsPriceInSpeech() {
        var input = HandlerTestHelper.intentInput("GetCurrentPriceIntent", "test-token");
        var price = new Price(0.25, "EUR", "NORMAL", "2026-04-22T14:00:00+02:00");
        when(tibberService.getCurrentPrice("test-token")).thenReturn(price);
        when(tibberService.formatPrice(0.25)).thenReturn("25,0 Cent");
        when(tibberService.formatLevel("NORMAL")).thenReturn("normal");

        var response = handler.handle(input);

        assertThat(response).isPresent();
        String speech = HandlerTestHelper.extractSpeech(response.get());
        assertThat(speech).contains("25,0 Cent").contains("normal");
    }

    @Test
    void handle_whenServiceThrowsException_returnsErrorSpeech() {
        var input = HandlerTestHelper.intentInput("GetCurrentPriceIntent", "test-token");
        when(tibberService.getCurrentPrice("test-token"))
                .thenThrow(new RuntimeException("API unavailable"));

        var response = handler.handle(input);

        assertThat(response).isPresent();
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("Problem");
    }

    @Test
    void handle_whenTibberRejectsToken_asksToRelinkWithCard() {
        var input = HandlerTestHelper.intentInput("GetCurrentPriceIntent", "test-token");
        when(tibberService.getCurrentPrice("test-token")).thenThrow(new TibberAuthenticationException("abgelehnt"));

        var response = handler.handle(input);

        assertThat(response).isPresent();
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("nicht mehr gültig");
        assertThat(response.get().getCard()).isInstanceOf(LinkAccountCard.class);
    }

    @Test
    void handle_withBlankToken_asksToLinkWithoutCallingTibber() {
        var input = HandlerTestHelper.intentInput("GetCurrentPriceIntent", "  ");

        var response = handler.handle(input);

        assertThat(HandlerTestHelper.extractSpeech(response.orElseThrow())).contains("verknüpfe");
        org.mockito.Mockito.verifyNoInteractions(tibberService);
    }
}
