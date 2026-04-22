
package org.tholian.alexa.tibber.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// KI-generiert: Unit-Tests für FallbackIntentHandler — prüft canHandle und "nicht verstanden"-Speech
class FallbackIntentHandlerTest {

    private FallbackIntentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new FallbackIntentHandler();
    }

    @Test
    void canHandle_matchesAmazonFallbackIntent() {
        var input = HandlerTestHelper.intentInput("AMAZON.FallbackIntent", "token");
        assertThat(handler.canHandle(input)).isTrue();
    }

    @Test
    void canHandle_doesNotMatchOtherIntent() {
        var input = HandlerTestHelper.intentInput("AMAZON.HelpIntent", "token");
        assertThat(handler.canHandle(input)).isFalse();
    }

    @Test
    void handle_returnsSpeechWithNotUnderstoodMessage() {
        var input = HandlerTestHelper.intentInput("AMAZON.FallbackIntent", "token");

        var response = handler.handle(input);

        assertThat(response).isPresent();
        // KI-generiert: Produktionscode liefert "Das habe ich leider nicht verstanden."
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("nicht verstanden");
    }
}
