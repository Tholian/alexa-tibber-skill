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
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        // KI-generiert: Echte Services ohne Netzwerkzugriff – beim Bauen wird Tibber nicht kontaktiert
        List<RequestHandler> handlers = SkillFactory.requestHandlers(
                SkillServices.create(URI.create("http://localhost/gql"), Clock.systemUTC()));

        assertThat(languageModel.path("intents")).isNotEmpty();
        for (JsonNode intent : languageModel.path("intents")) {
            String name = intent.path("name").asText();
            HandlerInput input = intentInput(name);
            assertThat(handlers).as("Request-Handler für Intent %s", name).anyMatch(h -> h.canHandle(input));
        }
    }

    @Test
    void everySlotReferenceInSamplesIsDeclared() throws IOException {
        JsonNode languageModel = new ObjectMapper().readTree(MODEL.toFile()).path("interactionModel").path("languageModel");
        Pattern reference = Pattern.compile("\\{(\\w+)\\}");

        for (JsonNode intent : languageModel.path("intents")) {
            Set<String> declared = new HashSet<>();
            List<String> samples = new ArrayList<>();
            intent.path("samples").forEach(sample -> samples.add(sample.asText()));
            for (JsonNode slot : intent.path("slots")) {
                declared.add(slot.path("name").asText());
                slot.path("samples").forEach(sample -> samples.add(sample.asText()));
            }
            for (String sample : samples) {
                Matcher matcher = reference.matcher(sample);
                while (matcher.find()) {
                    assertThat(declared).as("Slot {%s} in \"%s\" (%s)", matcher.group(1), sample, intent.path("name").asText())
                            .contains(matcher.group(1));
                }
            }
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

    @Test
    void dialogModelReferencesExistingIntentsSlotsAndPrompts() throws IOException {
        JsonNode model = new ObjectMapper().readTree(MODEL.toFile()).path("interactionModel");
        Set<String> promptIds = new HashSet<>();
        model.path("prompts").forEach(prompt -> promptIds.add(prompt.path("id").asText()));
        Map<String, Set<String>> slotsByIntent = new HashMap<>();
        for (JsonNode intent : model.path("languageModel").path("intents")) {
            Set<String> slots = new HashSet<>();
            intent.path("slots").forEach(slot -> slots.add(slot.path("name").asText()));
            slotsByIntent.put(intent.path("name").asText(), slots);
        }

        for (JsonNode intent : model.path("dialog").path("intents")) {
            String name = intent.path("name").asText();
            assertThat(slotsByIntent).as("Dialog-Intent %s im languageModel", name).containsKey(name);
            for (JsonNode slot : intent.path("slots")) {
                assertThat(slotsByIntent.get(name)).as("Slot %s in %s", slot.path("name").asText(), name)
                        .contains(slot.path("name").asText());
                String prompt = slot.path("prompts").path("elicitation").asText();
                if (!prompt.isEmpty()) {
                    assertThat(promptIds).as("Prompt %s", prompt).contains(prompt);
                }
            }
        }
    }
}
