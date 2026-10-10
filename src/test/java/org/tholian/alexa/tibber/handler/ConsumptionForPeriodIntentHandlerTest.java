package org.tholian.alexa.tibber.handler;

import com.amazon.ask.model.ui.LinkAccountCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tholian.alexa.tibber.service.DatePeriod;
import org.tholian.alexa.tibber.service.EnergyKind;
import org.tholian.alexa.tibber.service.EnergyResult;
import org.tholian.alexa.tibber.service.EnergyService;
import org.tholian.alexa.tibber.service.SpeechFormatter;
import org.tholian.alexa.tibber.service.TibberAuthenticationException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.CLOCK;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.extractSpeech;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.intentInput;
import static org.tholian.alexa.tibber.service.EnergyResult.Coverage.COMPLETE;
import static org.tholian.alexa.tibber.service.EnergyResult.Coverage.SO_FAR_TODAY;
import static org.tholian.alexa.tibber.service.EnergyResult.Coverage.UNTIL_YESTERDAY;

// KI-generiert: „Wie viel Strom habe ich gestern verbraucht?“ – EnergyService gemockt, Sprachtexte echt
@ExtendWith(MockitoExtension.class)
class ConsumptionForPeriodIntentHandlerTest {

    private static final String INTENT = "GetConsumptionForPeriodIntent";

    @Mock
    private EnergyService energyService;

    private ConsumptionForPeriodIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ConsumptionForPeriodIntentHandler(energyService, new SpeechFormatter(CLOCK));
    }

    @Test
    void handle_yesterday_speaksEnergyCostAndAverage() {
        stub("2026-10-09", new EnergyResult.Total(8.4, 2.31, "EUR", COMPLETE));

        assertThat(speech("2026-10-09")).contains("Gestern hast du 8,4 Kilowattstunden verbraucht und dafür 2,31 Euro bezahlt, "
                + "im Schnitt 27,5 Cent pro Kilowattstunde.");
    }

    @Test
    void handle_currentWeekWithoutCost_speaksEnergyUntilYesterday() {
        stub("2026-W41", new EnergyResult.Total(31.2, null, "EUR", UNTIL_YESTERDAY));

        assertThat(speech("2026-W41")).contains("Diese Woche hast du bis gestern 31,2 Kilowattstunden verbraucht.");
    }

    @Test
    void handle_today_saysBisher() {
        stub("2026-10-10", new EnergyResult.Total(3.2, 0.96, "EUR", SO_FAR_TODAY));

        assertThat(speech("2026-10-10")).contains("Heute hast du bisher 3,2 Kilowattstunden verbraucht und dafür 0,96 Euro bezahlt");
    }

    @Test
    void handle_zeroEnergy_omitsAverage() {
        stub("2026-10-09", new EnergyResult.Total(0.0, 0.0, "EUR", COMPLETE));

        assertThat(speech("2026-10-09")).contains("0 Kilowattstunden verbraucht und dafür 0,00 Euro bezahlt.")
                .doesNotContain("im Schnitt");
    }

    @Test
    void handle_future_saysSo() {
        stub("2026-10-11", new EnergyResult.Future());

        assertThat(speech("2026-10-11")).contains("liegt in der Zukunft");
    }

    @Test
    void handle_noData_saysSo() {
        stub("2026-10-09", new EnergyResult.NoData());

        assertThat(speech("2026-10-09")).contains("keine Verbrauchsdaten");
    }

    @Test
    void handle_tooOld_asksAgain() {
        stub("2000", new EnergyResult.TooOld());

        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("date", "2000"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("Diesen Zeitraum kann ich nicht auswerten. "
                + "Frag zum Beispiel: Wie viel Strom habe ich gestern verbraucht?");
        assertThat(response.getShouldEndSession()).isFalse();
    }

    @Test
    void handle_weekend_asksAgainWithoutCallingTibber() {
        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("date", "2026-W41-WE"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("Diesen Zeitraum kann ich nicht auswerten");
        verifyNoInteractions(energyService);
    }

    @Test
    void handle_withoutDate_asksAgain() {
        var response = handler.handle(intentInput(INTENT, "test-token")).orElseThrow();

        assertThat(extractSpeech(response)).contains("Diesen Zeitraum kann ich nicht auswerten");
    }

    @Test
    void handle_withNullToken_asksForLinkingWithCard() {
        var response = handler.handle(intentInput(INTENT, null, Map.of("date", "2026-10-09"))).orElseThrow();

        assertThat(response.getCard()).isInstanceOf(LinkAccountCard.class);
    }

    @Test
    void handle_whenTibberRejectsToken_asksToRelink() {
        when(energyService.total("test-token", DatePeriod.parse("2026-10-09").orElseThrow(), EnergyKind.CONSUMPTION))
                .thenThrow(new TibberAuthenticationException("abgelehnt"));

        assertThat(speech("2026-10-09")).contains("nicht mehr gültig");
    }

    @Test
    void handle_whenServiceFails_saysProblem() {
        when(energyService.total("test-token", DatePeriod.parse("2026-10-09").orElseThrow(), EnergyKind.CONSUMPTION))
                .thenThrow(new RuntimeException("API unavailable"));

        assertThat(speech("2026-10-09")).contains("Problem");
    }

    private void stub(String amazonDate, EnergyResult result) {
        when(energyService.total("test-token", DatePeriod.parse(amazonDate).orElseThrow(), EnergyKind.CONSUMPTION))
                .thenReturn(result);
    }

    private String speech(String amazonDate) {
        return extractSpeech(handler.handle(intentInput(INTENT, "test-token", Map.of("date", amazonDate))).orElseThrow());
    }
}
