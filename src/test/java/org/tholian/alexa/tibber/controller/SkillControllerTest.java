
package org.tholian.alexa.tibber.controller;

import com.amazon.ask.CustomSkill;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.ResponseEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * KI-generiert: Controller-Test für SkillController mit standaloneSetup MockMvc.
 * Spring Boot 4 hat @WebMvcTest und @MockBean entfernt. Stattdessen wird
 * MockMvcBuilders.standaloneSetup() mit einem lokalen ControllerAdvice verwendet –
 * kein Spring-Kontext nötig.
 *
 * Sicherheitshinweis: Der Controller verfügt aktuell über keine Alexa-Request-
 * Signaturverifizierung. Im Produktionsbetrieb muss dies nachgerüstet werden.
 */
@ExtendWith(MockitoExtension.class)
class SkillControllerTest {

    /**
     * KI-generiert: Minimaler ControllerAdvice für den standaloneSetup-Kontext.
     * Mappt alle unbehandelten Exceptions auf HTTP 500, wie es ein produktiver
     * Spring-Boot-Container mit DefaultErrorAttributes ebenfalls tun würde.
     */
    @RestControllerAdvice
    static class GlobalExceptionHandler {
        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        void handleAll(Exception ex) {
            // KI-generiert: Leerer Body ist für den Test ausreichend; Status 500 wird gesetzt.
        }
    }

    // KI-generiert: CustomSkill als Mockito-Mock – ersetzt den vollständigen Skill-Graph
    // (Handler, TibberService, RestClient) durch einen kontrollierbaren Platzhalter.
    @Mock
    private CustomSkill skill;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // KI-generiert: standaloneSetup registriert nur SkillController ohne Spring-Kontext.
        // GlobalExceptionHandler übernimmt die Exception-to-Status-Abbildung.
        SkillController controller = new SkillController(skill);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // KI-generiert: Minimaler valider Alexa-Request, der von ObjectMapperFactory deserialisiert werden kann
    private static final String VALID_ALEXA_REQUEST = """
            {
              "version": "1.0",
              "session": {
                "new": true,
                "sessionId": "amzn1.echo-api.session.test",
                "application": {"applicationId": "amzn1.ask.skill.test"},
                "user": {"userId": "test-user"}
              },
              "context": {
                "System": {
                  "application": {"applicationId": "amzn1.ask.skill.test"},
                  "user": {"userId": "test-user"},
                  "device": {"deviceId": "test-device", "supportedInterfaces": {}}
                }
              },
              "request": {
                "type": "LaunchRequest",
                "requestId": "amzn1.echo-api.request.test",
                "timestamp": "2026-04-22T12:00:00Z",
                "locale": "de-DE"
              }
            }
            """;

    @Test
    void postAlexa_withValidRequest_returns200AndResponseKey() throws Exception {
        // KI-generiert: ResponseEnvelope mit version "1.0" und leerem Response-Objekt mocken.
        // skill.invoke() liefert dieses Envelope zurück, SkillController serialisiert es als JSON.
        var mockEnvelope = ResponseEnvelope.builder()
                .withVersion("1.0")
                .withResponse(Response.builder().build())
                .build();
        when(skill.invoke(any(), any())).thenReturn(mockEnvelope);

        mockMvc.perform(post("/alexa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ALEXA_REQUEST))
                .andExpect(status().isOk())
                // KI-generiert: Explizite Content-Type-Assertion dokumentiert die Abhängigkeit von
                // Jacksons MappingJackson2HttpMessageConverter, der vor StringHttpMessageConverter
                // greift und application/json mit Charset-Angabe produziert.
                // Ohne diese Assertion wäre implizit angenommen, Jackson gewinne die Converter-Auswahl.
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.version").value("1.0"))
                .andExpect(jsonPath("$.response").exists());
    }

    @Test
    void postAlexa_withInvalidJson_returns500() throws Exception {
        // KI-generiert: Controller nimmt @RequestBody String, kein HTTP-Level-Parsing durch Spring.
        // Ungültiges JSON erreicht ObjectMapper.readValue() → Jackson-Exception → GlobalExceptionHandler
        // mappt dies auf HTTP 500.
        mockMvc.perform(post("/alexa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-valid-json"))
                .andExpect(status().isInternalServerError());
    }
}
