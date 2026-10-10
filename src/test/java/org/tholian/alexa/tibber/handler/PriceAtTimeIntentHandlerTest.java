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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.CLOCK;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.extractSpeech;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.intentInput;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.price;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.quarters;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.schedule;

// KI-generiert: „Was kostet Strom (morgen) um 18 Uhr?“ – jetzt = Samstag 10.10.2026, 10:00 Uhr Berlin
@ExtendWith(MockitoExtension.class)
class PriceAtTimeIntentHandlerTest {

    private static final String INTENT = "GetPriceAtTimeIntent";

    @Mock
    private PriceService priceService;

    private PriceAtTimeIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PriceAtTimeIntentHandler(priceService, new SpeechFormatter(CLOCK), CLOCK);
    }

    @Test
    void handle_timeLaterToday_speaksPriceAndLevel() {
        when(priceService.prices("test-token")).thenReturn(schedule(
                List.of(price(Instant.parse("2026-10-10T16:00:00Z"), 0.312, "EXPENSIVE"))));

        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("time", "18:10"))).orElseThrow();

        assertThat(extractSpeech(response)).contains(
                "Heute um 18:00 Uhr kostet Strom 31,2 Cent pro Kilowattstunde. Das Preisniveau ist teuer.");
    }

    @Test
    void handle_pastTimeWithoutDate_usesTomorrow() {
        when(priceService.prices("test-token")).thenReturn(schedule(quarters(Instant.parse("2026-10-11T06:00:00Z"), 0.18)));

        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("time", "08:00"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("Morgen um 08:00 Uhr kostet Strom 18,0 Cent");
    }

    @Test
    void handle_midnightWithoutDate_usesTomorrow() {
        // KI-generiert: 21:50 UTC = 23:50 Uhr Berlin – „um 0 Uhr“ meint die kommende Mitternacht
        Clock late = Clock.fixed(Instant.parse("2026-10-10T21:50:00Z"), ZoneOffset.UTC);
        var lateHandler = new PriceAtTimeIntentHandler(priceService, new SpeechFormatter(late), late);
        when(priceService.prices("test-token")).thenReturn(schedule(quarters(Instant.parse("2026-10-10T21:45:00Z"), 0.20, 0.15)));

        var response = lateHandler.handle(intentInput(INTENT, "test-token", Map.of("time", "00:00"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("Morgen um 00:00 Uhr kostet Strom 15,0 Cent");
    }

    @Test
    void handle_tomorrowWithoutPublishedPrices_saysNotPublished() {
        when(priceService.prices("test-token")).thenReturn(schedule(quarters(Instant.parse("2026-10-10T16:00:00Z"), 0.3)));

        var response = handler.handle(intentInput(INTENT, "test-token",
                Map.of("time", "18:00", "date", "2026-10-11"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("noch nicht veröffentlicht");
    }

    @Test
    void handle_dateBeyondTomorrow_saysOnlyTodayAndTomorrow_withoutCallingTibber() {
        var response = handler.handle(intentInput(INTENT, "test-token",
                Map.of("time", "18:00", "date", "2026-10-12"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("nur die Preise für heute und morgen");
        verifyNoInteractions(priceService);
    }

    @Test
    void handle_timeOfDayInsteadOfClockTime_asksAgain() {
        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("time", "EV"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("Zu welcher Uhrzeit");
        assertThat(response.getShouldEndSession()).isFalse();
        assertThat(response.getReprompt()).isNotNull();
        verifyNoInteractions(priceService);
    }

    @Test
    void handle_withoutTime_asksAgain() {
        var response = handler.handle(intentInput(INTENT, "test-token")).orElseThrow();

        assertThat(extractSpeech(response)).contains("Zu welcher Uhrzeit");
    }

    @Test
    void handle_withNullToken_asksForLinkingWithCard() {
        var response = handler.handle(intentInput(INTENT, null, Map.of("time", "18:00"))).orElseThrow();

        assertThat(response.getCard()).isInstanceOf(LinkAccountCard.class);
    }

    @Test
    void handle_whenTibberRejectsToken_asksToRelink() {
        when(priceService.prices("test-token")).thenThrow(new TibberAuthenticationException("abgelehnt"));

        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("time", "18:00"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("nicht mehr gültig");
    }

    @Test
    void quarterOf_roundsDownAndRejectsTimesOfDay() {
        assertThat(PriceAtTimeIntentHandler.quarterOf("18:44")).hasToString("18:30");
        assertThat(PriceAtTimeIntentHandler.quarterOf("MO")).isNull();
        assertThat(PriceAtTimeIntentHandler.quarterOf(null)).isNull();
    }

    @Test
    void handle_presentRefDate_isToday() {
        when(priceService.prices("test-token")).thenReturn(schedule(
                List.of(price(Instant.parse("2026-10-10T16:00:00Z"), 0.312, "EXPENSIVE"))));

        var response = handler.handle(intentInput(INTENT, "test-token",
                Map.of("time", "18:00", "date", "PRESENT_REF"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("Heute um 18:00 Uhr kostet Strom 31,2 Cent");
    }
}
