package org.tholian.alexa.tibber.handler;

import com.amazon.ask.model.ui.LinkAccountCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.service.PriceSchedule;
import org.tholian.alexa.tibber.service.PriceService;
import org.tholian.alexa.tibber.service.SpeechFormatter;
import org.tholian.alexa.tibber.service.TibberAuthenticationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.CLOCK;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.NOW;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.extractSpeech;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.intentInput;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.price;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.schedule;

// KI-generiert: Aktueller Preis – PriceService gemockt (externe Grenze), Formatierung mit echtem SpeechFormatter
@ExtendWith(MockitoExtension.class)
class CurrentPriceIntentHandlerTest {

    @Mock
    private PriceService priceService;

    private CurrentPriceIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CurrentPriceIntentHandler(priceService, new SpeechFormatter(CLOCK));
    }

    @Test
    void canHandle_matchesOnlyGetCurrentPriceIntent() {
        assertThat(handler.canHandle(intentInput("GetCurrentPriceIntent", "token"))).isTrue();
        assertThat(handler.canHandle(intentInput("GetLowestPriceIntent", "token"))).isFalse();
    }

    @Test
    void handle_withNullToken_asksForLinkingWithCard() {
        var response = handler.handle(intentInput("GetCurrentPriceIntent", null)).orElseThrow();

        assertThat(extractSpeech(response)).contains("verknüpfe");
        assertThat(response.getCard()).isInstanceOf(LinkAccountCard.class);
    }

    @Test
    void handle_withBlankToken_asksForLinking() {
        var response = handler.handle(intentInput("GetCurrentPriceIntent", "  ")).orElseThrow();

        assertThat(extractSpeech(response)).contains("verknüpfe");
    }

    @Test
    void handle_withCurrentPrice_speaksCentAndLevel() {
        Price current = price(NOW, 0.25, "NORMAL");
        when(priceService.prices("test-token")).thenReturn(schedule(current, List.of(current)));

        var response = handler.handle(intentInput("GetCurrentPriceIntent", "test-token")).orElseThrow();

        assertThat(extractSpeech(response))
                .contains("Der aktuelle Strompreis beträgt 25,0 Cent pro Kilowattstunde. Das Preisniveau ist normal.");
    }

    @Test
    void handle_withSekCurrency_speaksOere() {
        Price current = new Price(0.25, null, null, "SEK", "CHEAP", "2026-10-10T10:00:00+02:00");
        when(priceService.prices("test-token")).thenReturn(PriceSchedule.of(current, List.of(current), null));

        var response = handler.handle(intentInput("GetCurrentPriceIntent", "test-token")).orElseThrow();

        assertThat(extractSpeech(response)).contains("25,0 Öre").contains("günstig");
    }

    @Test
    void handle_withoutCurrentPrice_saysProblem() {
        when(priceService.prices("test-token")).thenReturn(schedule(List.of()));

        var response = handler.handle(intentInput("GetCurrentPriceIntent", "test-token")).orElseThrow();

        assertThat(extractSpeech(response)).contains("Problem");
    }

    @Test
    void handle_whenServiceFails_saysProblem() {
        when(priceService.prices("test-token")).thenThrow(new RuntimeException("API unavailable"));

        var response = handler.handle(intentInput("GetCurrentPriceIntent", "test-token")).orElseThrow();

        assertThat(extractSpeech(response)).contains("Problem");
    }

    @Test
    void handle_whenTibberRejectsToken_asksToRelinkWithCard() {
        when(priceService.prices("test-token")).thenThrow(new TibberAuthenticationException("abgelehnt"));

        var response = handler.handle(intentInput("GetCurrentPriceIntent", "test-token")).orElseThrow();

        assertThat(extractSpeech(response)).contains("nicht mehr gültig");
        assertThat(response.getCard()).isInstanceOf(LinkAccountCard.class);
    }
}
