
package org.tholian.alexa.tibber.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// KI-generiert: Unit-Tests für SessionEndedRequestHandler — prüft canHandle und leere Response ohne Speech
class SessionEndedRequestHandlerTest {

    private SessionEndedRequestHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SessionEndedRequestHandler();
    }

    @Test
    void canHandle_matchesSessionEndedRequest() {
        var input = HandlerTestHelper.sessionEndedInput();
        assertThat(handler.canHandle(input)).isTrue();
    }

    @Test
    void canHandle_doesNotMatchLaunchRequest() {
        var input = HandlerTestHelper.launchInput();
        assertThat(handler.canHandle(input)).isFalse();
    }

    @Test
    void handle_returnsEmptyResponse() {
        var input = HandlerTestHelper.sessionEndedInput();

        var response = handler.handle(input);

        // KI-generiert: SessionEnded liefert absichtlich eine leere Response ohne Speech
        assertThat(response).isPresent();
        assertThat(response.get().getOutputSpeech()).isNull();
    }
}
