
package org.tholian.alexa.tibber.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// KI-generiert: Unit-Tests für HelpIntentHandler — prüft canHandle und Hilfe-Speech mit Tibber-Branding
class HelpIntentHandlerTest {

    private HelpIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new HelpIntentHandler();
    }

    @Test
    void canHandle_matchesAmazonHelpIntent() {
        var input = HandlerTestHelper.intentInput("AMAZON.HelpIntent", "token");
        assertThat(handler.canHandle(input)).isTrue();
    }

    @Test
    void canHandle_doesNotMatchOtherIntent() {
        var input = HandlerTestHelper.intentInput("GetCurrentPriceIntent", "token");
        assertThat(handler.canHandle(input)).isFalse();
    }

    @Test
    void handle_returnsSpeechWithHelpText() {
        var input = HandlerTestHelper.intentInput("AMAZON.HelpIntent", "token");

        var response = handler.handle(input);

        assertThat(response).isPresent();
        // KI-generiert: Produktionscode liefert "Mit dem Tibber Strompreis Skill kannst du..."
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("Tibber");
    }
}
