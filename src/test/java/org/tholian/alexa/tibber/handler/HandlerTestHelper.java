
package org.tholian.alexa.tibber.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Context;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.SessionEndedRequest;
import com.amazon.ask.model.User;
import com.amazon.ask.model.interfaces.system.SystemState;
import com.amazon.ask.model.ui.SsmlOutputSpeech;

// KI-generiert: Zentrale Builder-Hilfen für Handler-Tests, reduziert Boilerplate pro Testklasse
class HandlerTestHelper {

    static HandlerInput intentInput(String intentName, String accessToken) {
        var userBuilder = User.builder();
        if (accessToken != null) {
            userBuilder.withAccessToken(accessToken);
        }
        var user = userBuilder.build();
        var system = SystemState.builder().withUser(user).build();
        var context = Context.builder().withSystem(system).build();
        var intent = Intent.builder().withName(intentName).build();
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
