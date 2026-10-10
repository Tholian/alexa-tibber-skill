package org.tholian.alexa.tibber.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.service.PriceService;
import org.tholian.alexa.tibber.service.SpeechFormatter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.CLOCK;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.NOW;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.extractSpeech;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.intentInput;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.blocks;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.price;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.schedule;

// KI-generiert: Urteil nach Tibber-Preisniveau der laufenden Viertelstunde, ggf. mit Hinweis auf die günstigste Stunde
@ExtendWith(MockitoExtension.class)
class IsPriceCheapIntentHandlerTest {

    private static final String INTENT = "IsPriceCheapIntent";

    @Mock
    private PriceService priceService;

    private IsPriceCheapIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new IsPriceCheapIntentHandler(priceService, new SpeechFormatter(CLOCK), CLOCK);
    }

    @Test
    void handle_cheap_saysYesWithoutHint() {
        Price current = price(NOW, 0.21, "CHEAP");
        when(priceService.prices("test-token")).thenReturn(schedule(current, List.of(current)));

        String speech = extractSpeech(handler.handle(intentInput(INTENT, "test-token")).orElseThrow());

        assertThat(speech).contains("Ja, Strom ist gerade günstig: 21,0 Cent pro Kilowattstunde.").doesNotContain("Stunde beginnt");
    }

    @Test
    void handle_veryCheap_saysSehrGuenstig() {
        Price current = price(NOW, 0.05, "VERY_CHEAP");
        when(priceService.prices("test-token")).thenReturn(schedule(current, List.of(current)));

        assertThat(extractSpeech(handler.handle(intentInput(INTENT, "test-token")).orElseThrow()))
                .contains("Ja, Strom ist gerade sehr günstig");
    }

    @Test
    void handle_normal_namesCheapestHour() {
        Price current = price(NOW, 0.263, "NORMAL");
        when(priceService.prices("test-token")).thenReturn(schedule(current, blocks(NOW, 4, 0.263, 0.194)));

        String speech = extractSpeech(handler.handle(intentInput(INTENT, "test-token")).orElseThrow());

        assertThat(speech).contains("Strom kostet gerade 26,3 Cent, das ist ein normales Preisniveau.")
                .contains("Die günstigste Stunde beginnt heute um 11:00 Uhr mit im Schnitt 19,4 Cent.");
    }

    @Test
    void handle_expensive_saysNo() {
        Price current = price(NOW, 0.341, "EXPENSIVE");
        when(priceService.prices("test-token")).thenReturn(schedule(current, blocks(NOW, 4, 0.341, 0.2)));

        assertThat(extractSpeech(handler.handle(intentInput(INTENT, "test-token")).orElseThrow()))
                .contains("Nein, Strom ist gerade teuer: 34,1 Cent.");
    }

    @Test
    void handle_withoutLevel_speaksPriceOnly() {
        Price current = new Price(0.263, null, null, "EUR", null, "2026-10-10T10:00:00+02:00");
        when(priceService.prices("test-token")).thenReturn(schedule(current, List.of(current)));

        assertThat(extractSpeech(handler.handle(intentInput(INTENT, "test-token")).orElseThrow()))
                .contains("Strom kostet gerade 26,3 Cent pro Kilowattstunde.");
    }

    @Test
    void handle_withoutCurrentPrice_saysProblem() {
        when(priceService.prices("test-token")).thenReturn(schedule(List.of()));

        assertThat(extractSpeech(handler.handle(intentInput(INTENT, "test-token")).orElseThrow())).contains("Problem");
    }
}
