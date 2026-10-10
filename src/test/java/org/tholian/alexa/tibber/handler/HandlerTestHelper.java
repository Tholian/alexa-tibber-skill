
package org.tholian.alexa.tibber.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Context;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.SessionEndedRequest;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.User;
import com.amazon.ask.model.interfaces.system.SystemState;
import com.amazon.ask.model.ui.SsmlOutputSpeech;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Collectors;

// KI-generiert: Zentrale Builder-Hilfen für Handler-Tests, reduziert Boilerplate pro Testklasse
class HandlerTestHelper {

    // KI-generiert: Feste Uhr für alle Handler-Tests – Samstag, 10.10.2026, 10:00 Uhr in Berlin (CEST)
    static final Instant NOW = Instant.parse("2026-10-10T08:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    static HandlerInput intentInput(String intentName, String accessToken) {
        return intentInput(intentName, accessToken, Map.of());
    }

    // KI-generiert: Intent mit Slot-Werten, wie Alexa sie nach der Auflösung liefert (z. B. AMAZON.DATE → "2026-10-11")
    static HandlerInput intentInput(String intentName, String accessToken, Map<String, String> slotValues) {
        var userBuilder = User.builder();
        if (accessToken != null) {
            userBuilder.withAccessToken(accessToken);
        }
        var system = SystemState.builder().withUser(userBuilder.build()).build();
        var context = Context.builder().withSystem(system).build();
        Map<String, Slot> slots = slotValues.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> Slot.builder().withName(e.getKey()).withValue(e.getValue()).build()));
        var intent = Intent.builder().withName(intentName).withSlots(slots).build();
        var request = IntentRequest.builder()
                .withRequestId("test-request-id")
                .withIntent(intent)
                .build();
        var envelope = RequestEnvelope.builder()
                .withRequest(request)
                .withContext(context)
                .build();
        return HandlerInput.builder().withRequestEnvelope(envelope).build();
    }

    static HandlerInput launchInput() {
        var request = LaunchRequest.builder().withRequestId("test-launch-id").build();
        var envelope = RequestEnvelope.builder().withRequest(request).build();
        return HandlerInput.builder().withRequestEnvelope(envelope).build();
    }

    static HandlerInput sessionEndedInput() {
        var request = SessionEndedRequest.builder().withRequestId("test-session-end-id").build();
        var envelope = RequestEnvelope.builder().withRequest(request).build();
        return HandlerInput.builder().withRequestEnvelope(envelope).build();
    }

    // KI-generiert: ASK SDK withSpeech() erzeugt immer SsmlOutputSpeech mit <speak>-Tags
    static String extractSpeech(com.amazon.ask.model.Response response) {
        var output = response.getOutputSpeech();
        if (output instanceof SsmlOutputSpeech ssml) {
            return ssml.getSsml();
        }
        return "";
    }
}
