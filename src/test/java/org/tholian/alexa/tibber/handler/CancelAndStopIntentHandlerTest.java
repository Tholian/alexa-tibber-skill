
package org.tholian.alexa.tibber.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// KI-generiert: Unit-Tests für CancelAndStopIntentHandler — prüft canHandle für Cancel und Stop, sowie Abschluss-Speech mit Session-Ende-Flag
class CancelAndStopIntentHandlerTest {

    private CancelAndStopIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CancelAndStopIntentHandler();
    }

    @Test
    void canHandle_matchesAmazonCancelIntent() {
        var input = HandlerTestHelper.intentInput("AMAZON.CancelIntent", "token");
        assertThat(handler.canHandle(input)).isTrue();
    }

    @Test
    void canHandle_matchesAmazonStopIntent() {
        var input = HandlerTestHelper.intentInput("AMAZON.StopIntent", "token");
        assertThat(handler.canHandle(input)).isTrue();
    }

    @Test
    void canHandle_doesNotMatchOtherIntent() {
        var input = HandlerTestHelper.intentInput("AMAZON.HelpIntent", "token");
        assertThat(handler.canHandle(input)).isFalse();
    }

    @Test
    void handle_returnsFarewellSpeechWithSessionEnd() {
        var input = HandlerTestHelper.intentInput("AMAZON.CancelIntent", "token");

        var response = handler.handle(input);

        assertThat(response).isPresent();
        // KI-generiert: Produktionscode liefert "Auf Wiedersehen!"
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("Wiedersehen");
        // KI-generiert: shouldEndSession muss true sein beim Stop/Cancel
        assertThat(response.get().getShouldEndSession()).isTrue();
    }
}
