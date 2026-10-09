package org.tholian.alexa.tibber.handler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// KI-generiert: Unit-Tests für den Fallback-Exception-Handler – jede Exception wird in eine gesprochene Antwort übersetzt
class GenericExceptionHandlerTest {

    private final GenericExceptionHandler handler = new GenericExceptionHandler();

    @Test
    void canHandle_acceptsEveryThrowable() {
        assertThat(handler.canHandle(HandlerTestHelper.launchInput(), new IllegalStateException("x"))).isTrue();
    }

    @Test
    void handle_returnsApologyAndKeepsSessionOpen() {
        var response = handler.handle(HandlerTestHelper.launchInput(), new IllegalStateException("boom"));

        assertThat(response).isPresent();
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("hat nicht geklappt");
        assertThat(response.get().getShouldEndSession()).isFalse();
        assertThat(response.get().getReprompt()).isNotNull();
    }
}
