package org.tholian.alexa.tibber.config;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KI-generiert: Wächter zwischen Interaction Model und Code.
 * Jeder Intent aus skill-package/.../de-DE.json muss von mindestens einem Request-Handler bedient werden –
 * sonst landet der Nutzer im Fallback (so war es bisher bei AMAZON.NavigateHomeIntent).
 */
class InteractionModelCoverageTest {

    private static final Path MODEL = Path.of("skill-package", "interactionModels", "custom", "de-DE.json");

    @Test
    void everyIntentOfTheInteractionModelHasARequestHandler() throws IOException {
        JsonNode languageModel = new ObjectMapper().readTree(MODEL.toFile()).path("interactionModel").path("languageModel");
        List<RequestHandler> handlers = SkillFactory.requestHandlers(null);

        assertThat(languageModel.path("intents")).isNotEmpty();
        for (JsonNode intent : languageModel.path("intents")) {
            String name = intent.path("name").asText();
            HandlerInput input = intentInput(name);
            assertThat(handlers).as("Request-Handler für Intent %s", name).anyMatch(h -> h.canHandle(input));
        }
    }

    @Test
    void invocationNameIsTibberStrompreis() throws IOException {
        JsonNode languageModel = new ObjectMapper().readTree(MODEL.toFile()).path("interactionModel").path("languageModel");

        assertThat(languageModel.path("invocationName").asText()).isEqualTo("tibber strompreis");
    }

    private static HandlerInput intentInput(String intentName) {
        var request = IntentRequest.builder()
                .withRequestId("coverage-test")
                .withIntent(Intent.builder().withName(intentName).build())
                .build();
        var envelope = RequestEnvelope.builder().withRequest(request).build();
        return HandlerInput.builder().withRequestEnvelope(envelope).build();
    }
}
