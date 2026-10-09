package org.tholian.alexa.tibber.config;

import com.amazon.ask.CustomSkill;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.ui.SsmlOutputSpeech;
import com.amazon.ask.util.impl.ObjectMapperFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tholian.alexa.tibber.testsupport.AlexaRequestJson;
import org.tholian.alexa.tibber.testsupport.FakeTibberServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tholian.alexa.tibber.testsupport.TibberJson.homeResponse;
import static org.tholian.alexa.tibber.testsupport.TibberJson.price;

/**
 * KI-generiert: Prüft die komplette Verdrahtung (Handler, Interceptoren, Exception-Handler, TibberService)
 * mit echten Objekten. Nur die Tibber API ist durch den lokalen FakeTibberServer ersetzt.
 */
class SkillFactoryTest {

    private static final String SKILL_ID = "amzn1.ask.skill.test";

    private FakeTibberServer tibber;
    private CustomSkill skill;

    @BeforeEach
    void setUp() {
        tibber = new FakeTibberServer();
        skill = SkillFactory.create(new SkillSettings(SKILL_ID, tibber.uri()));
    }

    @AfterEach
    void tearDown() {
        tibber.close();
    }

    @Test
    void launchRequest_isAnsweredWithWelcome() throws Exception {
        assertThat(speech(invoke(AlexaRequestJson.launch(SKILL_ID)))).contains("Willkommen");
    }

    @Test
    void currentPriceIntent_queriesTibberAndAnswersInCent() throws Exception {
        tibber.respondWith(200, homeResponse(price(0.25, "NORMAL", "2026-06-15T10:00:00Z"), "[]", "null", "[]"));

        Response response = invoke(AlexaRequestJson.intent(SKILL_ID, "GetCurrentPriceIntent", "test-token"));

        assertThat(speech(response)).contains("25,0 Cent").contains("normal");
        assertThat(tibber.lastAuthorization()).isEqualTo("Bearer test-token");
    }

    @Test
    void intentWithoutAccessToken_asksForAccountLinkingWithoutCallingTibber() throws Exception {
        Response response = invoke(AlexaRequestJson.intent(SKILL_ID, "GetCurrentPriceIntent", null));

        assertThat(speech(response)).contains("verknüpfe");
        assertThat(tibber.requestCount()).isZero();
    }

    @Test
    void navigateHomeIntent_endsSession() throws Exception {
        Response response = invoke(AlexaRequestJson.intent(SKILL_ID, "AMAZON.NavigateHomeIntent", "test-token"));

        assertThat(response.getShouldEndSession()).isTrue();
    }

    @Test
    void unknownIntent_isAnsweredWithApology() throws Exception {
        Response response = invoke(AlexaRequestJson.intent(SKILL_ID, "SomethingUnknownIntent", "test-token"));

        assertThat(speech(response)).contains("hat nicht geklappt");
    }

    @Test
    void requestHandlers_containsAllNineHandlers() {
        assertThat(SkillFactory.requestHandlers(null)).hasSize(9);
    }

    private Response invoke(String json) throws Exception {
        RequestEnvelope envelope = ObjectMapperFactory.getMapper().readValue(json, RequestEnvelope.class);
        return skill.invoke(envelope, null).getResponse();
    }

    private static String speech(Response response) {
        return response.getOutputSpeech() instanceof SsmlOutputSpeech ssml ? ssml.getSsml() : "";
    }
}
