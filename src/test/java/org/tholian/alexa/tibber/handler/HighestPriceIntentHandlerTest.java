package org.tholian.alexa.tibber.handler;

import com.amazon.ask.model.ui.LinkAccountCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tholian.alexa.tibber.service.PriceService;
import org.tholian.alexa.tibber.service.SpeechFormatter;
import org.tholian.alexa.tibber.service.TibberAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.CLOCK;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.NOW;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.extractSpeech;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.intentInput;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.blocks;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.quarters;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.schedule;

// KI-generiert: Teuerste Stunde = teuerstes 60-Minuten-Fenster ab der laufenden Viertelstunde
@ExtendWith(MockitoExtension.class)
class HighestPriceIntentHandlerTest {

    @Mock
    private PriceService priceService;

    private HighestPriceIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new HighestPriceIntentHandler(priceService, new SpeechFormatter(CLOCK), CLOCK);
    }

    @Test
    void canHandle_matchesOnlyGetHighestPriceIntent() {
        assertThat(handler.canHandle(intentInput("GetHighestPriceIntent", "token"))).isTrue();
        assertThat(handler.canHandle(intentInput("GetLowestPriceIntent", "token"))).isFalse();
    }

    @Test
    void handle_withNullToken_asksForLinkingWithCard() {
        var response = handler.handle(intentInput("GetHighestPriceIntent", null)).orElseThrow();

        assertThat(extractSpeech(response)).contains("verknüpfe");
        assertThat(response.getCard()).isInstanceOf(LinkAccountCard.class);
    }

    @Test
    void handle_withExpensiveHourLater_namesStartAndAverage() {
        when(priceService.prices("test-token")).thenReturn(schedule(blocks(NOW, 4, 0.20, 0.45, 0.30)));

        var response = handler.handle(intentInput("GetHighestPriceIntent", "test-token")).orElseThrow();

        assertThat(extractSpeech(response))
                .contains("Die teuerste Stunde beginnt heute um 11:00 Uhr, im Schnitt 45,0 Cent pro Kilowattstunde.");
    }

    @Test
    void handle_withLessThanAnHourOfPrices_saysNoData() {
        when(priceService.prices("test-token")).thenReturn(schedule(quarters(NOW, 0.1, 0.1)));

        var response = handler.handle(intentInput("GetHighestPriceIntent", "test-token")).orElseThrow();

        assertThat(extractSpeech(response)).contains("keine Preisdaten");
    }

    @Test
    void handle_whenServiceFails_saysProblem() {
        when(priceService.prices("test-token")).thenThrow(new RuntimeException("API unavailable"));

        var response = handler.handle(intentInput("GetHighestPriceIntent", "test-token")).orElseThrow();

        assertThat(extractSpeech(response)).contains("Problem");
    }

    @Test
    void handle_whenTibberRejectsToken_asksToRelinkWithCard() {
        when(priceService.prices("test-token")).thenThrow(new TibberAuthenticationException("abgelehnt"));

        var response = handler.handle(intentInput("GetHighestPriceIntent", "test-token")).orElseThrow();

        assertThat(extractSpeech(response)).contains("nicht mehr gültig");
        assertThat(response.getCard()).isInstanceOf(LinkAccountCard.class);
    }
}
