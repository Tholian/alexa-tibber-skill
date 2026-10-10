package org.tholian.alexa.tibber.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tholian.alexa.tibber.service.PriceService;
import org.tholian.alexa.tibber.service.SpeechFormatter;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.CLOCK;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.NOW;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.extractSpeech;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.intentInput;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.quarters;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.schedule;

// KI-generiert: „Wie teuer wird Strom morgen?“ – Minimum, Maximum, Durchschnitt mit Uhrzeiten
@ExtendWith(MockitoExtension.class)
class DayPriceSummaryIntentHandlerTest {

    private static final String INTENT = "GetDayPriceSummaryIntent";
    private static final Instant TOMORROW_MIDNIGHT = Instant.parse("2026-10-10T22:00:00Z");

    @Mock
    private PriceService priceService;

    private DayPriceSummaryIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DayPriceSummaryIntentHandler(priceService, new SpeechFormatter(CLOCK), CLOCK);
    }

    @Test
    void handle_tomorrow_speaksRangeAverageAndTimes() {
        when(priceService.prices("test-token")).thenReturn(schedule(quarters(TOMORROW_MIDNIGHT, 0.25, 0.182, 0.347, 0.25)));

        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("date", "2026-10-11"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("Morgen kostet Strom zwischen 18,2 Cent und 34,7 Cent, "
                + "im Schnitt 25,7 Cent pro Kilowattstunde. Am günstigsten ist es um 00:15 Uhr, am teuersten um 00:30 Uhr.");
    }

    @Test
    void handle_todayWithoutDate_coversOnlyRestOfDay() {
        when(priceService.prices("test-token")).thenReturn(schedule(quarters(NOW.minus(Duration.ofMinutes(15)), 0.01, 0.30, 0.20)));

        var response = handler.handle(intentInput(INTENT, "test-token")).orElseThrow();

        assertThat(extractSpeech(response)).contains("Für den Rest des Tages kostet Strom zwischen 20,0 Cent und 30,0 Cent");
    }

    @Test
    void handle_tomorrowNotPublished_saysSo() {
        when(priceService.prices("test-token")).thenReturn(schedule(quarters(NOW, 0.30)));

        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("date", "2026-10-11"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("noch nicht veröffentlicht");
    }

    @Test
    void handle_otherDate_saysOnlyTodayAndTomorrow_withoutCallingTibber() {
        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("date", "2026-10-20"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("nur die Preise für heute und morgen");
        verifyNoInteractions(priceService);
    }

    @Test
    void handle_weekInsteadOfDay_saysOnlyTodayAndTomorrow() {
        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("date", "2026-W41"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("nur die Preise für heute und morgen");
    }

    @Test
    void handle_presentRef_isTodayAndSpeaksRestOfDay() {
        when(priceService.prices("test-token")).thenReturn(schedule(quarters(NOW.minus(Duration.ofMinutes(15)), 0.01, 0.30, 0.20)));

        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("date", "PRESENT_REF"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("Für den Rest des Tages kostet Strom zwischen 20,0 Cent und 30,0 Cent");
    }
}
