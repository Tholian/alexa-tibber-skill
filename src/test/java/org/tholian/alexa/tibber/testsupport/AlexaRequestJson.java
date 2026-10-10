package org.tholian.alexa.tibber.testsupport;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * KI-generiert: Alexa-Request-Envelopes als JSON, wie Alexa sie an die Lambda-Funktion sendet.
 * Skill-ID steht sowohl in session.application als auch in context.System.application.
 */
public final class AlexaRequestJson {

    private AlexaRequestJson() {
    }

    public static String launch(String skillId) {
        return envelope(skillId, null, """
                {"type":"LaunchRequest","requestId":"amzn1.echo-api.request.test",
                 "timestamp":"2026-06-15T10:00:00Z","locale":"de-DE"}
                """);
    }

    /** KI-generiert: accessToken darf null sein (Account Linking nicht erfolgt). */
    public static String intent(String skillId, String intentName, String accessToken) {
        return envelope(skillId, accessToken, """
                {"type":"IntentRequest","requestId":"amzn1.echo-api.request.test",
                 "timestamp":"2026-06-15T10:00:00Z","locale":"de-DE",
                 "intent":{"name":"%s","confirmationStatus":"NONE"}}
                """.formatted(intentName));
    }

    /** KI-generiert: Intent mit Slot-Werten (Werte ohne Anführungszeichen/Backslashes – reine Testdaten). */
    public static String intent(String skillId, String intentName, String accessToken, Map<String, String> slots) {
        String slotJson = slots.entrySet().stream()
                .map(e -> "\"%1$s\":{\"name\":\"%1$s\",\"value\":\"%2$s\",\"confirmationStatus\":\"NONE\"}"
                        .formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining(","));
        return envelope(skillId, accessToken, """
                {"type":"IntentRequest","requestId":"amzn1.echo-api.request.test",
                 "timestamp":"2026-06-15T10:00:00Z","locale":"de-DE",
                 "intent":{"name":"%s","confirmationStatus":"NONE","slots":{%s}}}
                """.formatted(intentName, slotJson));
    }

    private static String envelope(String skillId, String accessToken, String request) {
        String token = accessToken == null ? "" : ",\"accessToken\":\"" + accessToken + "\"";
        return """
                {
                  "version": "1.0",
                  "session": {
                    "new": true,
                    "sessionId": "amzn1.echo-api.session.test",
                    "application": {"applicationId": "%1$s"},
                    "user": {"userId": "amzn1.ask.account.test"%2$s}
                  },
                  "context": {
                    "System": {
                      "application": {"applicationId": "%1$s"},
                      "user": {"userId": "amzn1.ask.account.test"%2$s},
                      "device": {"deviceId": "test-device", "supportedInterfaces": {}}
                    }
                  },
                  "request": %3$s
                }
                """.formatted(skillId, token, request);
    }
}
