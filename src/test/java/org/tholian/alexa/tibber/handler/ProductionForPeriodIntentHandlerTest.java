package org.tholian.alexa.tibber.handler;

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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.CLOCK;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.extractSpeech;
import static org.tholian.alexa.tibber.handler.HandlerTestHelper.intentInput;

// KI-generiert: „Wie viel habe ich im September eingespeist?“ – Einspeisung und Erlös
@ExtendWith(MockitoExtension.class)
class ProductionForPeriodIntentHandlerTest {

    private static final String INTENT = "GetProductionForPeriodIntent";

    @Mock
    private EnergyService energyService;

    private ProductionForPeriodIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProductionForPeriodIntentHandler(energyService, new SpeechFormatter(CLOCK));
    }

    @Test
    void handle_pastMonth_speaksProductionAndProfit() {
        stub("2026-09", new EnergyResult.Total(312.5, 24.8, "EUR", EnergyResult.Coverage.COMPLETE));

        assertThat(speech("2026-09")).contains("Im September hast du 312,5 Kilowattstunden eingespeist und dafür 24,80 Euro erhalten.");
    }

    @Test
    void handle_noData_saysNoProductionData() {
        stub("2026-09", new EnergyResult.NoData());

        assertThat(speech("2026-09")).contains("keine Einspeisedaten");
    }

    @Test
    void handle_future_saysSo() {
        stub("2026-11", new EnergyResult.Future());

        assertThat(speech("2026-11")).contains("liegt in der Zukunft");
    }

    @Test
    void handle_tooOld_asksAgainWithProductionExample() {
        stub("2000", new EnergyResult.TooOld());

        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("date", "2000"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("Diesen Zeitraum kann ich nicht auswerten. "
                + "Frag zum Beispiel: Wie viel habe ich letzte Woche eingespeist?");
        assertThat(response.getShouldEndSession()).isFalse();
    }

    @Test
    void handle_weekend_asksAgainWithProductionExample() {
        var response = handler.handle(intentInput(INTENT, "test-token", Map.of("date", "2026-W41-WE"))).orElseThrow();

        assertThat(extractSpeech(response)).contains("Wie viel habe ich letzte Woche eingespeist?");
    }

    private void stub(String amazonDate, EnergyResult result) {
        when(energyService.total("test-token", DatePeriod.parse(amazonDate).orElseThrow(), EnergyKind.PRODUCTION))
                .thenReturn(result);
    }

    private String speech(String amazonDate) {
        return extractSpeech(handler.handle(intentInput(INTENT, "test-token", Map.of("date", amazonDate))).orElseThrow());
    }
}
