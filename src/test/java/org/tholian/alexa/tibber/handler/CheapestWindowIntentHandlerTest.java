package org.tholian.alexa.tibber.handler;

import com.amazon.ask.model.dialog.DelegateDirective;
import com.amazon.ask.model.dialog.ElicitSlotDirective;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tholian.alexa.tibber.service.PriceService;
import org.tholian.alexa.tibber.service.SpeechFormatter;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.CLOCK;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.NOW;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.extractSpeech;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.intentInput;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.blocks;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.quarters;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.schedule;

// KI-generiert: „Wann sind die günstigsten 3 Stunden?“ – Dauer aus AMAZON.DURATION, Raster 15 Minuten
@ExtendWith(MockitoExtension.class)
class CheapestWindowIntentHandlerTest {

    private static final String INTENT = "GetCheapestWindowIntent";

    @Mock
    private PriceService priceService;

    private CheapestWindowIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CheapestWindowIntentHandler(priceService, new SpeechFormatter(CLOCK), CLOCK);
    }

    @Test
    void handle_threeHours_namesStartAndAverage() {
        when(priceService.prices("test-token")).thenReturn(schedule(blocks(NOW, 4, 0.30, 0.10, 0.10, 0.10, 0.30)));

        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("duration", "PT3H"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("Der günstigste Zeitraum für 3 Stunden beginnt heute um 11:00 Uhr, "
                + "im Schnitt 10,0 Cent pro Kilowattstunde.");
    }

    @Test
    void handle_ninetyMinutes_usesSixQuarters() {
        when(priceService.prices("test-token")).thenReturn(schedule(quarters(NOW, 0.3, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.3)));

        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("duration", "PT90M"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("für eine Stunde und 30 Minuten beginnt heute um 10:15 Uhr");
    }

    @Test
    void handle_twentyMinutes_roundsUpToHalfHour() {
        when(priceService.prices("test-token")).thenReturn(schedule(quarters(NOW, 0.3, 0.1, 0.1, 0.3)));

        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("duration", "PT20M"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("für 30 Minuten beginnt heute um 10:15 Uhr");
    }

    @Test
    void handle_runningWindowIsCheapest_saysSofort() {
        when(priceService.prices("test-token")).thenReturn(schedule(blocks(NOW, 4, 0.10, 0.30)));

        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("duration", "PT1H"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("für eine Stunde beginnt sofort");
    }

    @Test
    void handle_durationLongerThanData_namesLastKnownSlot() {
        when(priceService.prices("test-token")).thenReturn(schedule(blocks(NOW, 4, 0.2, 0.2)));

        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("duration", "PT30H"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("Für 30 Stunden reichen die bekannten Preise nicht. "
                + "Tibber hat Preise bis heute um 11:45 Uhr veröffentlicht.");
    }

    @Test
    void handle_withoutDuration_delegatesDialogToAlexa() {
        var response = handler.handle(intentInput(INTENT, "test-token")).orElseThrow();

        assertThat(response.getDirectives()).singleElement().isInstanceOf(DelegateDirective.class);
        verifyNoInteractions(priceService);
    }

    @Test
    void handle_unparseableDuration_asksAgain() {
        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("duration", "P1W"))).orElseThrow();

        assertThat(response.getDirectives()).singleElement().isInstanceOfSatisfying(ElicitSlotDirective.class, directive -> {
            assertThat(directive.getSlotToElicit()).isEqualTo("duration");
            assertThat(directive.getUpdatedIntent().getSlots().get("duration").getValue()).isNull();
        });
        assertThat(extractSpeech(response)).contains("Wie lange soll das Gerät laufen?");
        assertThat(response.getShouldEndSession()).isNotEqualTo(true);
        verifyNoInteractions(priceService);
    }

    @Test
    void quartersOf_roundsUpAndRejectsInvalidValues() {
        assertThat(CheapestWindowIntentHandler.quartersOf("PT3H")).isEqualTo(12);
        assertThat(CheapestWindowIntentHandler.quartersOf("PT1M")).isEqualTo(1);
        assertThat(CheapestWindowIntentHandler.quartersOf("P30D")).isEqualTo(CheapestWindowIntentHandler.MAX_QUARTERS);
        assertThat(CheapestWindowIntentHandler.quartersOf("PT0S")).isNull();
        assertThat(CheapestWindowIntentHandler.quartersOf("PT0.5S")).isEqualTo(1);
        assertThat(CheapestWindowIntentHandler.quartersOf("PT2562047788015215H")).isEqualTo(CheapestWindowIntentHandler.MAX_QUARTERS);
        assertThat(CheapestWindowIntentHandler.quartersOf("drei Stunden")).isNull();
    }
}
