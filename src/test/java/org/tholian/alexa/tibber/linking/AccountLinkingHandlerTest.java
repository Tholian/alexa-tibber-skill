package org.tholian.alexa.tibber.linking;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.tholian.alexa.tibber.config.SkillSettings;
import org.tholian.alexa.tibber.service.TibberAccountService;
import org.tholian.alexa.tibber.testsupport.FakeTibberServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.tholian.alexa.tibber.testsupport.ApiGatewayEvents.form;
import static org.tholian.alexa.tibber.testsupport.ApiGatewayEvents.get;
import static org.tholian.alexa.tibber.testsupport.ApiGatewayEvents.getRaw;
import static org.tholian.alexa.tibber.testsupport.ApiGatewayEvents.post;
import static org.tholian.alexa.tibber.testsupport.ApiGatewayEvents.postBase64;
import static org.tholian.alexa.tibber.testsupport.ApiGatewayEvents.request;
import static org.tholian.alexa.tibber.testsupport.TibberJson.homesResponse;
import static org.tholian.alexa.tibber.testsupport.TibberJson.unauthenticatedResponse;

/**
 * KI-generiert: End-to-End-Tests der Account-Linking-Lambda (API Gateway Event rein, HTTP-Antwort raus).
 * Tibber ist durch den lokalen FakeTibberServer ersetzt. Alle Werte sind Fantasiewerte.
 */
class AccountLinkingHandlerTest {

    private static final String CLIENT_ID = "tibber-skill-client";
    private static final String REDIRECT = "https://layla.amazon.com/api/skill/link/M2ABCDEFGHIJ";
    private static final String HOST = "abc123.execute-api.eu-west-1.amazonaws.com";
    private static final String ORIGIN = "https://" + HOST;
    private static final String TOKEN = "AbCdEf0123456789-AbCdEf0123456789";

    private final ObjectMapper mapper = new ObjectMapper();
    private FakeTibberServer tibber;
    private AccountLinkingHandler handler;

    @BeforeEach
    void setUp() {
        tibber = new FakeTibberServer();
        handler = new AccountLinkingHandler(Map.of(
                LinkingSettings.ENV_CLIENT_ID, CLIENT_ID,
                LinkingSettings.ENV_REDIRECT_URIS, REDIRECT,
                LinkingSettings.ENV_PRIVACY_POLICY_URL, "https://example.org/datenschutz",
                SkillSettings.ENV_TIBBER_API_URL, tibber.uri().toString()));
    }

    @AfterEach
    void tearDown() {
        tibber.close();
    }

    private static Map<String, String> query() {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("state", "abc-123");
        query.put("client_id", CLIENT_ID);
        query.put("response_type", "token");
        query.put("redirect_uri", REDIRECT);
        return query;
    }

    private static Map<String, String> formFields(String token) {
        Map<String, String> fields = query();
        fields.put("tibber_token", token);
        return fields;
    }

    private JsonNode invoke(String event) throws IOException {
        var out = new ByteArrayOutputStream();
        handler.handleRequest(new ByteArrayInputStream(event.getBytes(UTF_8)), out, null);
        return mapper.readTree(out.toByteArray());
    }

    // ---------------------------------------------------------------- GET

    @Test
    void get_withValidRequest_rendersFormWithSecurityHeaders() throws IOException {
        JsonNode response = invoke(get("/link", query(), HOST));

        assertThat(response.path("statusCode").asInt()).isEqualTo(200);
        JsonNode headers = response.path("headers");
        assertThat(headers.path("Content-Type").asText()).isEqualTo("text/html; charset=utf-8");
        assertThat(headers.path("Content-Security-Policy").asText()).isEqualTo(LinkingPage.CSP);
        assertThat(headers.path("X-Frame-Options").asText()).isEqualTo("DENY");
        assertThat(headers.path("Cache-Control").asText()).isEqualTo("no-store");
        assertThat(headers.path("Referrer-Policy").asText()).isEqualTo("same-origin");
        assertThat(headers.path("X-Content-Type-Options").asText()).isEqualTo("nosniff");
        assertThat(headers.path("Strict-Transport-Security").asText()).startsWith("max-age=");
        String body = response.path("body").asText();
        assertThat(body).contains("name=\"state\" value=\"abc-123\"")
                .contains("name=\"tibber_token\"")
                .contains("type=\"password\"")
                .contains("vollen Zugriff")
                .contains("href=\"https://example.org/datenschutz\"")
                .doesNotContain("<script");
    }

    @Test
    void get_withDuplicateQueryParameters_isRejected() throws IOException {
        // KI-generiert: queryStringParameters wären gültig (API Gateway fasst Duplikate zusammen); entscheidend ist der Raw-Query-String
        var event = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(get("/link", query(), HOST));
        event.put("rawQueryString", form(query()) + "&state=other");

        JsonNode response = invoke(event.toString());

        assertThat(response.path("statusCode").asInt()).isEqualTo(400);
        assertThat(response.path("headers").has("Location")).isFalse();
    }

    @Test
    void get_withPercentEncodedRedirectUriInRawQuery_isAccepted() throws IOException {
        String raw = "state=abc-123&client_id=" + CLIENT_ID + "&response_type=token&redirect_uri="
                     + java.net.URLEncoder.encode(REDIRECT, UTF_8);

        JsonNode response = invoke(getRaw("/link", raw, HOST));

        assertThat(response.path("statusCode").asInt()).isEqualTo(200);
        assertThat(response.path("body").asText()).contains("name=\"redirect_uri\" value=\"" + REDIRECT + "\"");
    }

    @Test
    void get_withForeignRedirectUri_showsErrorAndNeverRedirects() throws IOException {
        var query = query();
        query.put("redirect_uri", "https://evil.example/cb");

        JsonNode response = invoke(get("/link", query, HOST));

        assertThat(response.path("statusCode").asInt()).isEqualTo(400);
        assertThat(response.path("headers").has("Location")).isFalse();
        assertThat(response.path("body").asText()).contains("Unzulässige Weiterleitungsadresse").doesNotContain("evil.example");
    }

    @Test
    void get_withRedirectUriSuffixTrick_isRejected() throws IOException {
        var query = query();
        query.put("redirect_uri", REDIRECT + ".evil.example");

        JsonNode response = invoke(get("/link", query, HOST));

        assertThat(response.path("statusCode").asInt()).isEqualTo(400);
        assertThat(response.path("headers").has("Location")).isFalse();
    }

    @Test
    void get_withWrongClientIdOrResponseType_isRejected() throws IOException {
        var wrongClient = query();
        wrongClient.put("client_id", "someone-else");
        assertThat(invoke(get("/link", wrongClient, HOST)).path("statusCode").asInt()).isEqualTo(400);

        var wrongType = query();
        wrongType.put("response_type", "code");
        assertThat(invoke(get("/link", wrongType, HOST)).path("statusCode").asInt()).isEqualTo(400);
    }

    @Test
    void get_escapesReflectedState() throws IOException {
        var query = query();
        query.put("state", "\"><script>alert(1)</script>");

        String body = invoke(get("/link", query, HOST)).path("body").asText();

        assertThat(body).doesNotContain("<script>alert(1)</script>")
                .contains("&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    @Test
    void get_withControlCharactersInState_isRejected() throws IOException {
        var query = query();
        query.put("state", "abc\r\nSet-Cookie: x=y");

        JsonNode response = invoke(get("/link", query, HOST));

        assertThat(response.path("statusCode").asInt()).isEqualTo(400);
        assertThat(response.path("headers").has("Set-Cookie")).isFalse();
    }

    @Test
    void unknownPath_returns404AndUnsupportedMethod_returns405() throws IOException {
        assertThat(invoke(get("/other", query(), HOST)).path("statusCode").asInt()).isEqualTo(404);
        assertThat(invoke(request("PUT", "/link", HOST)).path("statusCode").asInt()).isEqualTo(405);
    }

    // ---------------------------------------------------------------- POST

    @Test
    void post_withValidToken_redirectsWithTokenInFragment() throws IOException {
        tibber.respondWith(200, homesResponse("home-1"));

        JsonNode response = invoke(post("/link", form(formFields(TOKEN)), HOST, ORIGIN));

        assertThat(response.path("statusCode").asInt()).isEqualTo(302);
        assertThat(response.path("headers").path("Location").asText())
                .isEqualTo(REDIRECT + "#state=abc-123&access_token=" + TOKEN + "&token_type=Bearer");
        assertThat(response.path("headers").path("Cache-Control").asText()).isEqualTo("no-store");
        assertThat(tibber.lastAuthorization()).isEqualTo("Bearer " + TOKEN);
        assertThat(tibber.lastRequestBody()).contains("homes").doesNotContain("priceInfo");
    }

    @Test
    void post_withBase64Body_isAccepted() throws IOException {
        tibber.respondWith(200, homesResponse("home-1"));

        JsonNode response = invoke(postBase64("/link", form(formFields(TOKEN)), HOST, ORIGIN));

        assertThat(response.path("statusCode").asInt()).isEqualTo(302);
    }

    @Test
    void post_withoutOriginHeader_isAccepted() throws IOException {
        tibber.respondWith(200, homesResponse("home-1"));

        assertThat(invoke(post("/link", form(formFields(TOKEN)), HOST, null)).path("statusCode").asInt()).isEqualTo(302);
    }

    @Test
    void post_whenTibberRejectsToken_rerendersFormWithoutRedirect() throws IOException {
        tibber.respondWith(200, unauthenticatedResponse());

        JsonNode response = invoke(post("/link", form(formFields(TOKEN)), HOST, ORIGIN));

        assertThat(response.path("statusCode").asInt()).isEqualTo(400);
        assertThat(response.path("headers").has("Location")).isFalse();
        assertThat(response.path("body").asText()).contains("nicht akzeptiert")
                .contains("name=\"state\" value=\"abc-123\"")
                .doesNotContain(TOKEN);
    }

    @Test
    void post_whenTibberFails_showsRetryMessage() throws IOException {
        tibber.respondWith(500, "{}");

        JsonNode response = invoke(post("/link", form(formFields(TOKEN)), HOST, ORIGIN));

        assertThat(response.path("statusCode").asInt()).isEqualTo(502);
        assertThat(response.path("headers").has("Location")).isFalse();
        assertThat(response.path("body").asText()).contains("später erneut").doesNotContain(TOKEN);
    }

    @Test
    void post_whenTibberReturnsIncompleteViewer_showsBadGatewayPageWithSecurityHeaders() throws IOException {
        tibber.respondWith(200, "{\"data\":{\"viewer\":null}}");

        JsonNode response = invoke(post("/link", form(formFields(TOKEN)), HOST, ORIGIN));

        assertThat(response.path("statusCode").asInt()).isEqualTo(502);
        assertThat(response.path("headers").has("Location")).isFalse();
        assertThat(response.path("headers").path("Content-Security-Policy").asText()).isEqualTo(LinkingPage.CSP);
        assertThat(response.path("body").asText()).contains("später erneut").doesNotContain(TOKEN);
    }

    @Test
    void post_whenUnexpectedRuntimeExceptionOccurs_returnsGenericErrorPageAndLogsOnlyClassName() throws IOException {
        TibberAccountService failing = mock(TibberAccountService.class);
        doThrow(new IllegalStateException("geheim " + TOKEN)).when(failing).verifyToken(anyString());
        var failingHandler = new AccountLinkingHandler(
                LinkingSettings.fromEnvironment(Map.of(
                        LinkingSettings.ENV_CLIENT_ID, CLIENT_ID,
                        LinkingSettings.ENV_REDIRECT_URIS, REDIRECT)),
                failing);
        var root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        root.addAppender(appender);
        JsonNode response;
        try {
            var out = new ByteArrayOutputStream();
            failingHandler.handleRequest(new ByteArrayInputStream(
                    post("/link", form(formFields(TOKEN)), HOST, ORIGIN).getBytes(UTF_8)), out, null);
            response = mapper.readTree(out.toByteArray());
        } finally {
            root.detachAppender(appender);
        }

        assertThat(response.path("statusCode").asInt()).isEqualTo(500);
        assertThat(response.path("headers").has("Location")).isFalse();
        assertThat(response.path("headers").path("Content-Security-Policy").asText()).isEqualTo(LinkingPage.CSP);
        assertThat(response.path("body").asText()).contains("interner Fehler").doesNotContain(TOKEN);
        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage()).contains("IllegalStateException"));
        assertThat(appender.list).allSatisfy(event -> {
            assertThat(event.getFormattedMessage()).doesNotContain(TOKEN);
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    @Test
    void post_withMalformedToken_doesNotCallTibber() throws IOException {
        JsonNode response = invoke(post("/link", form(formFields("zu kurz")), HOST, ORIGIN));

        assertThat(response.path("statusCode").asInt()).isEqualTo(400);
        assertThat(response.path("body").asText()).contains("ungültiges Format");
        assertThat(tibber.requestCount()).isZero();
    }

    @Test
    void post_withForeignOrigin_isRejected() throws IOException {
        JsonNode response = invoke(post("/link", form(formFields(TOKEN)), HOST, "https://evil.example"));

        assertThat(response.path("statusCode").asInt()).isEqualTo(403);
        assertThat(tibber.requestCount()).isZero();
    }

    @Test
    void post_withNullOrigin_isRejected() throws IOException {
        JsonNode response = invoke(post("/link", form(formFields(TOKEN)), HOST, "null"));

        assertThat(response.path("statusCode").asInt()).isEqualTo(403);
        assertThat(tibber.requestCount()).isZero();
    }

    @Test
    void post_withManipulatedRedirectUri_isRejected() throws IOException {
        var fields = formFields(TOKEN);
        fields.put("redirect_uri", "https://evil.example/cb");

        JsonNode response = invoke(post("/link", form(fields), HOST, ORIGIN));

        assertThat(response.path("statusCode").asInt()).isEqualTo(400);
        assertThat(response.path("headers").has("Location")).isFalse();
        assertThat(tibber.requestCount()).isZero();
    }

    @Test
    void post_withDuplicateParameters_isRejected() throws IOException {
        String body = form(formFields(TOKEN)) + "&redirect_uri=" + java.net.URLEncoder.encode("https://evil.example/cb", UTF_8);

        JsonNode response = invoke(post("/link", body, HOST, ORIGIN));

        assertThat(response.path("statusCode").asInt()).isEqualTo(400);
        assertThat(tibber.requestCount()).isZero();
    }

    @Test
    void post_withTooLargeBody_isRejected() throws IOException {
        String body = form(formFields(TOKEN)) + "&pad=" + "a".repeat(AccountLinkingHandler.MAX_BODY_BYTES);

        JsonNode response = invoke(post("/link", body, HOST, ORIGIN));

        assertThat(response.path("statusCode").asInt()).isEqualTo(413);
        assertThat(tibber.requestCount()).isZero();
    }

    @Test
    void post_neverLogsOrEchoesToken() throws IOException {
        var root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        root.addAppender(appender);
        try {
            tibber.respondWith(200, homesResponse("home-1"));
            invoke(post("/link", form(formFields(TOKEN)), HOST, ORIGIN));
            tibber.respondWith(200, unauthenticatedResponse());
            invoke(post("/link", form(formFields(TOKEN)), HOST, ORIGIN));
            tibber.respondWith(500, "{}");
            invoke(post("/link", form(formFields(TOKEN)), HOST, ORIGIN));
        } finally {
            root.detachAppender(appender);
        }

        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list).allSatisfy(event -> {
            assertThat(event.getFormattedMessage()).doesNotContain(TOKEN);
            if (event.getThrowableProxy() != null) {
                assertThat(String.valueOf(event.getThrowableProxy().getMessage())).doesNotContain(TOKEN);
            }
        });
    }
}
