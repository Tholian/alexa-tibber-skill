package org.tholian.alexa.tibber.handler;

import com.amazon.ask.model.ui.LinkAccountCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tholian.alexa.tibber.model.ConsumptionNode;
import org.tholian.alexa.tibber.model.LiveMeasurement;
import org.tholian.alexa.tibber.service.CurrentUsage;
import org.tholian.alexa.tibber.service.Deadline;
import org.tholian.alexa.tibber.service.LiveService;
import org.tholian.alexa.tibber.service.SpeechFormatter;
import org.tholian.alexa.tibber.service.TibberAuthenticationException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.CLOCK;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.extractSpeech;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.intentInput;

// KI-generiert: Live-Leistung mit Pulse, sonst letzte abgerechnete Stunde – LiveService gemockt, Sprachtexte echt
@ExtendWith(MockitoExtension.class)
class CurrentConsumptionIntentHandlerTest {

    private static final String INTENT = "GetCurrentConsumptionIntent";

    @Mock
    private LiveService liveService;

    private CurrentConsumptionIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CurrentConsumptionIntentHandler(liveService, new SpeechFormatter(CLOCK), Duration.ofSeconds(6));
    }

    @Test
    void handle_live_speaksPowerAndTodaysConsumption() {
        stub(new CurrentUsage.Live(new LiveMeasurement("2026-10-10T10:00:00+02:00", 1239.6, 0.0, 6.3, 1.72, "EUR")));

        assertThat(speech()).contains("Gerade verbrauchst du 1240 Watt. Heute hast du bisher 6,3 Kilowattstunden für 1,72 Euro verbraucht.");
    }

    @Test
    void handle_liveWithProduction_mentionsFeedIn() {
        stub(new CurrentUsage.Live(new LiveMeasurement("2026-10-10T10:00:00+02:00", 0.0, 2100.0, 6.3, 1.72, "EUR")));

        assertThat(speech()).contains("Gerade verbrauchst du 0 Watt und speist 2100 Watt ein.");
    }

    @Test
    void handle_liveWithoutCost_omitsCost() {
        stub(new CurrentUsage.Live(new LiveMeasurement("2026-10-10T10:00:00+02:00", 500.0, null, 6.3, null, "EUR")));

        assertThat(speech()).contains("Heute hast du bisher 6,3 Kilowattstunden verbraucht.");
    }

    @Test
    void handle_lastHour_speaksSettledHour() {
        stub(new CurrentUsage.LastHour(new ConsumptionNode("2026-10-10T09:00:00+02:00", "2026-10-10T10:00:00+02:00", 0.84, 0.21, "EUR")));

        assertThat(speech()).contains("In der letzten abgerechneten Stunde ab 09:00 Uhr hast du 0,84 Kilowattstunden verbraucht "
                + "und dafür 0,21 Euro bezahlt.");
    }

    @Test
    void handle_lastHourIncomplete_saysIncomplete() {
        stub(new CurrentUsage.LastHour(new ConsumptionNode("2026-10-10T09:00:00+02:00", "2026-10-10T10:00:00+02:00", null, null, null)));

        assertThat(speech()).contains("noch keine vollständigen Verbrauchsdaten");
    }

    @Test
    void handle_noData_saysNoData() {
        stub(new CurrentUsage.LastHour(null));

        assertThat(speech()).contains("keine Verbrauchsdaten");
    }

    @Test
    void handle_withNullToken_asksForLinkingWithCard() {
        var response = handler.handle(intentInput(INTENT, null)).orElseThrow();

        assertThat(response.getCard()).isInstanceOf(LinkAccountCard.class);
    }

    @Test
    void handle_whenTibberRejectsToken_asksToRelink() {
        when(liveService.currentUsage(eq("test-token"), any(Deadline.class))).thenThrow(new TibberAuthenticationException("abgelehnt"));

        assertThat(speech()).contains("nicht mehr gültig");
    }

    @Test
    void handle_whenServiceFails_saysProblem() {
        when(liveService.currentUsage(eq("test-token"), any(Deadline.class))).thenThrow(new RuntimeException("API unavailable"));

        assertThat(speech()).contains("Problem");
    }

    private void stub(CurrentUsage usage) {
        when(liveService.currentUsage(eq("test-token"), any(Deadline.class))).thenReturn(usage);
    }

    private String speech() {
        return extractSpeech(handler.handle(intentInput(INTENT, "test-token")).orElseThrow());
    }
}
