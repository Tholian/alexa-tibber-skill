package org.tholian.alexa.tibber.handler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// KI-generiert: Der Auffang-Handler akzeptiert jeden Request und antwortet mit einer Entschuldigung
class UnhandledRequestHandlerTest {

    private final UnhandledRequestHandler handler = new UnhandledRequestHandler();

    @Test
    void canHandle_acceptsEveryRequest() {
        assertThat(handler.canHandle(HandlerTestHelper.intentInput("SomethingUnknownIntent", "token"))).isTrue();
        assertThat(handler.canHandle(HandlerTestHelper.launchInput())).isTrue();
    }

    @Test
    void handle_returnsApologyAndKeepsSessionOpen() {
        var response = handler.handle(HandlerTestHelper.intentInput("SomethingUnknownIntent", "token"));

        assertThat(response).isPresent();
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("hat nicht geklappt");
        assertThat(response.get().getShouldEndSession()).isFalse();
        assertThat(response.get().getReprompt()).isNotNull();
    }

    @Test
    void handle_worksForNonIntentRequests() {
        var response = handler.handle(HandlerTestHelper.launchInput());

        assertThat(response).isPresent();
    }
}
