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
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.price;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.schedule;

// KI-generiert: Aufschlüsselung des aktuellen Preises in Energiepreis und Steuern/Abgaben (Tibber-Felder energy/tax)
@ExtendWith(MockitoExtension.class)
class PriceBreakdownIntentHandlerTest {

    private static final String INTENT = "GetPriceBreakdownIntent";

    @Mock
    private PriceService priceService;

    private PriceBreakdownIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PriceBreakdownIntentHandler(priceService, new SpeechFormatter(CLOCK));
    }

    @Test
    void handle_withParts_speaksEnergyAndTax() {
        Price current = new Price(0.234, 0.121, 0.113, "EUR", "NORMAL", "2026-10-10T10:00:00+02:00");
        when(priceService.prices("test-token")).thenReturn(schedule(current, List.of(current)));

        assertThat(extractSpeech(handler.handle(intentInput(INTENT, "test-token")).orElseThrow())).contains(
                "Von 23,4 Cent pro Kilowattstunde sind 12,1 Cent Energiepreis und 11,3 Cent Steuern und Abgaben.");
    }

    @Test
    void handle_withoutParts_saysNoBreakdown() {
        Price current = price(NOW, 0.234);
        when(priceService.prices("test-token")).thenReturn(schedule(current, List.of(current)));

        assertThat(extractSpeech(handler.handle(intentInput(INTENT, "test-token")).orElseThrow()))
                .contains("keine Aufschlüsselung");
    }
}
