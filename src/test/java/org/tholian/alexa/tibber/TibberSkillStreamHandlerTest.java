package org.tholian.alexa.tibber;

import com.amazon.ask.exception.AskSdkException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tholian.alexa.tibber.config.SkillSettings;
import org.tholian.alexa.tibber.testsupport.AlexaRequestJson;
import org.tholian.alexa.tibber.testsupport.FakeTibberServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.tholian.alexa.tibber.testsupport.TibberJson.homeResponse;
import static org.tholian.alexa.tibber.testsupport.TibberJson.price;

/**
 * KI-generiert: End-to-End-Test des Lambda-Einstiegs – JSON rein, JSON raus, genau wie in AWS Lambda.
 * Ersetzt den früheren Controller-Test. Die Tibber API ist durch den lokalen FakeTibberServer ersetzt.
 */
class TibberSkillStreamHandlerTest {

    private static final String SKILL_ID = "amzn1.ask.skill.test";

    private FakeTibberServer tibber;
    private TibberSkillStreamHandler handler;

    @BeforeEach
    void setUp() {
        tibber = new FakeTibberServer();
        handler = new TibberSkillStreamHandler(Map.of(
                SkillSettings.ENV_SKILL_ID, SKILL_ID,
                SkillSettings.ENV_TIBBER_API_URL, tibber.uri().toString()));
    }

    @AfterEach
    void tearDown() {
        tibber.close();
    }

    @Test
    void handleRequest_launchRequest_writesWelcomeResponseJson() throws IOException {
        String json = invoke(AlexaRequestJson.launch(SKILL_ID));

        assertThat(json).contains("\"version\":\"1.0\"").contains("Willkommen");
    }

    @Test
    void handleRequest_currentPriceIntent_queriesTibberAndAnswersInCent() throws IOException {
        tibber.respondWith(200, homeResponse(price(0.25, "NORMAL", "2026-06-15T10:00:00Z"), "[]", "null", "[]"));

        String json = invoke(AlexaRequestJson.intent(SKILL_ID, "GetCurrentPriceIntent", "test-token"));

        assertThat(json).contains("25,0 Cent").contains("normal");
        assertThat(tibber.lastAuthorization()).isEqualTo("Bearer test-token");
    }

    @Test
    void handleRequest_fromForeignSkill_isRejected() {
        // KI-generiert: SICHERHEIT – Requests mit fremder Skill-ID dürfen nicht beantwortet werden
        var out = new ByteArrayOutputStream();

        assertThatThrownBy(() -> handler.handleRequest(
                new ByteArrayInputStream(AlexaRequestJson.launch("amzn1.ask.skill.foreign").getBytes(UTF_8)), out, null))
                .isInstanceOf(AskSdkException.class);
        assertThat(out.size()).isZero();
    }

    @Test
    void handleRequest_withInvalidJson_throws() {
        var out = new ByteArrayOutputStream();

        assertThatThrownBy(() -> handler.handleRequest(new ByteArrayInputStream("not-json".getBytes(UTF_8)), out, null));
    }

    @Test
    void constructor_withoutSkillId_failsFast() {
        assertThatThrownBy(() -> new TibberSkillStreamHandler(Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ALEXA_SKILL_ID");
    }

    private String invoke(String requestJson) throws IOException {
        var out = new ByteArrayOutputStream();
        handler.handleRequest(new ByteArrayInputStream(requestJson.getBytes(UTF_8)), out, null);
        return out.toString(UTF_8);
    }
}
