
package org.tholian.alexa.tibber.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// KI-generiert: Unit-Tests für LaunchRequestHandler — prüft canHandle und Willkommens-Speech
class LaunchRequestHandlerTest {

    private LaunchRequestHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LaunchRequestHandler();
    }

    @Test
    void canHandle_matchesLaunchRequest() {
        var input = HandlerTestHelper.launchInput();
        assertThat(handler.canHandle(input)).isTrue();
    }

    @Test
    void canHandle_doesNotMatchIntentRequest() {
        var input = HandlerTestHelper.intentInput("GetCurrentPriceIntent", "token");
        assertThat(handler.canHandle(input)).isFalse();
    }

    @Test
    void handle_returnsSpeechWithWelcomeMessage() {
        var input = HandlerTestHelper.launchInput();

        var response = handler.handle(input);

        assertThat(response).isPresent();
        // KI-generiert: Produktionscode liefert "Willkommen beim Tibber Strompreis Skill! ..."
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("Willkommen").contains("Waschmaschine");
    }
}
