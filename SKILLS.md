# Test-Patterns – Alexa Tibber Skill

Wiederverwendbare Patterns für Tests in diesem Projekt. Aus der ersten JUnit-6-Test-Session erarbeitet.

---

## ASK SDK Handler testen

### HandlerInput aufbauen

`HandlerInput` wird über ASK SDK Builder konstruiert (nicht per Mockito). Die zentrale Hilfsklasse liegt in `src/test/java/org/tholian/alexa/tibber/handler/HandlerTestHelper.java`.

```java
// Intent-Request mit optionalem Access-Token
static HandlerInput intentInput(String intentName, String accessToken) {
    var userBuilder = User.builder();
    if (accessToken != null) userBuilder.withAccessToken(accessToken);
    var system = SystemState.builder().withUser(userBuilder.build()).build();
    var context = Context.builder().withSystem(system).build();
    var request = IntentRequest.builder()
            .withRequestId("test-id")
            .withIntent(Intent.builder().withName(intentName).build())
            .build();
    var envelope = RequestEnvelope.builder().withRequest(request).withContext(context).build();
    return HandlerInput.builder().withRequestEnvelope(envelope).build();
}

// LaunchRequest (kein Context nötig für einfache Handler)
static HandlerInput launchInput() { ... }
static HandlerInput sessionEndedInput() { ... }
```

### Speech extrahieren

`withSpeech()` im ASK SDK erzeugt **immer** `SsmlOutputSpeech` (mit `<speak>`-Tags), nie `PlainTextOutputSpeech`. `contains()`-Assertions funktionieren trotzdem korrekt da der gesuchte Text im SSML-String enthalten ist.

```java
static String extractSpeech(Response response) {
    var output = response.getOutputSpeech();
    if (output instanceof SsmlOutputSpeech ssml) return ssml.getSsml();
    return "";
}
```

### Handler-Test-Struktur

```java
@ExtendWith(MockitoExtension.class)
class SomeIntentHandlerTest {
    @Mock PriceService priceService;
    private SomeIntentHandler handler;

    @BeforeEach void setUp() { handler = new SomeIntentHandler(priceService, new SpeechFormatter(CLOCK), CLOCK); }

    @Test void canHandle_matchesCorrectIntent() { ... }
    @Test void canHandle_doesNotMatchOtherIntent() { ... }
    @Test void handle_withNullToken_returnsAccountLinkingHint() { ... }
    @Test void handle_success() { ... }
    @Test void handle_empty_returnsNoDataSpeech() { ... }    // nur bei Optional-Rückgabe
    @Test void handle_serviceThrowsException_returnsErrorSpeech() { ... }
}
```

---

## Tibber API in Tests: `FakeTibberServer`

Die Tibber API wird nicht gemockt, sondern durch einen echten lokalen HTTP-Server ersetzt (`src/test/java/org/tholian/alexa/tibber/testsupport/FakeTibberServer.java`, basiert auf dem JDK-`HttpServer`, Zufallsport, nur Loopback). JSON-Fixtures liefert `TibberJson`.

```java
private FakeTibberServer server;
private PriceService service;

@BeforeEach void setUp() {
    server = new FakeTibberServer();
    service = new PriceService(new TibberGraphQlClient(HttpClient.newHttpClient(), new ObjectMapper(), server.uri(),
            Duration.ofMillis(500)));
}

@AfterEach void tearDown() { server.close(); }

// Pro Test:
server.respondWith(200, priceInfoResponse(price(0.25, "NORMAL", "2026-10-10T10:00:00+02:00"), "[]", "null"));
server.respondTo("\"resolution\":\"MONTHLY\"", consumptionResponse(...)); // mehrere Abfragen in einem Ablauf
server.delayResponsesBy(Duration.ofSeconds(2));                            // Timeout-Test
assertThat(server.lastAuthorization()).isEqualTo("Bearer test-token");
assertThat(server.lastRequestBody()).contains("\"resolution\":\"DAILY\"");   // GraphQL-Variablen
```

## Preise und Uhrzeit in Tests: `PriceFixtures` und feste Uhr

`PriceFixtures.quarters(start, 0.30, 0.10, …)` erzeugt Viertelstunden ab einem `Instant`, `blocks(start, 4, 0.30, 0.10)` Stundenblöcke. Handler-Tests nutzen `HandlerTestHelper.CLOCK` (Samstag, 10.10.2026, 10:00 Uhr Berlin), einen echten `SpeechFormatter` und gemockte Services:

```java
handler = new LowestPriceIntentHandler(priceService, new SpeechFormatter(CLOCK), CLOCK);
when(priceService.prices("test-token")).thenReturn(schedule(blocks(NOW, 4, 0.30, 0.10)));
var response = handler.handle(intentInput("GetLowestPriceIntent", "test-token", Map.of())).orElseThrow();
```

Slots: `HandlerTestHelper.intentInput(intent, token, Map.of("date", "2026-10-11"))`; im End-to-End-Test `AlexaRequestJson.intent(skillId, intent, token, Map.of(...))`.

## Live-Daten in Tests: `FakeTibberWebSocketServer`

GraphQL-over-WebSocket-Server (`graphql-transport-ws`) auf Basis von Java-WebSocket (nur `testImplementation`), Loopback, Zufallsport:

```java
ws = new FakeTibberWebSocketServer();
ws.behave(Behavior.NO_ACK);                      // MEASUREMENT, PING_THEN_MEASUREMENT, FRAGMENTED_MEASUREMENT, ERROR, CLOSE_ON_SUBSCRIBE, NO_ACK
http.respondWith(200, livePrerequisiteResponse(ws.uri().toString(), true, "[]"));
assertThat(ws.awaitClientClosed(Duration.ofSeconds(2))).isTrue();
assertThat(ws.userAgent()).isEqualTo("alexa-tibber-skill/1.0");
```

---

## Lambda-Einstieg testen

`TibberSkillStreamHandler` hat einen package-privaten Konstruktor mit Umgebungs-Map – so lässt sich der echte Lambda-Einstieg ohne Manipulation von `System.getenv()` testen. Alexa-Requests liefert `AlexaRequestJson`.

```java
handler = new TibberSkillStreamHandler(Map.of(
        SkillSettings.ENV_SKILL_ID, "amzn1.ask.skill.test",
        SkillSettings.ENV_TIBBER_API_URL, tibber.uri().toString()));

var out = new ByteArrayOutputStream();
handler.handleRequest(new ByteArrayInputStream(AlexaRequestJson.launch("amzn1.ask.skill.test").getBytes(UTF_8)), out, null);
assertThat(out.toString(UTF_8)).contains("Willkommen");
```

---

## Integrationstest gegen echte Tibber API

```java
@EnabledIfEnvironmentVariable(named = "TIBBER_DEMO_TOKEN", matches = ".+")
class TibberApiIntegrationTest { ... }
```

Manueller Aufruf (Demo-Token aus dem Tibber Developer Portal, nie einchecken):
```bash
# Git Bash
TIBBER_DEMO_TOKEN=<demo-token> ./gradlew test --tests "*.TibberApiIntegrationTest"
```
```powershell
# PowerShell
$env:TIBBER_DEMO_TOKEN = '<demo-token>'; .\gradlew.bat test --tests "*.TibberApiIntegrationTest"
```

**Tibber GraphQL-Schema Hinweise:**
- `unit` ist **kein** gültiges Feld auf `Consumption` → `GRAPHQL_VALIDATION_FAILED`. Gültige Felder: `from`, `to`, `consumption`, `cost`, `consumptionUnit`
- Demo-Account liefert Preise in **SEK**, nicht EUR — Assertions auf `total` daher mit `isFinite()` statt `isGreaterThan(0.0)` (Spotpreise können 0 oder negativ sein)
- Bei `UNAUTHENTICATED`-Fehler: Demo-Token ist abgelaufen → neuen Token in Tibber API-Dokumentation suchen

---

## Timestamps in Tests

`EnergyService`, `SpeechFormatter` und die zeitabhängigen Handler bekommen eine `Clock` injiziert (`PriceService` liefert nur Daten, „jetzt“ kommt aus der Clock der Handler/des `SpeechFormatter`). Für Tests mit „ab jetzt“-Logik die Uhr fest stellen und feste Zeitpunkte davor/danach verwenden:

```java
private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");
Clock.fixed(NOW, ZoneOffset.UTC)
"2026-06-15T09:00:00Z"   // Vergangenheit
"2026-06-15T11:00:00Z"   // Zukunft
```

Für Tests von `SpeechFormatter.clockTime`/`time`: Datum muss sicher in CEST (Sommer) oder CET (Winter) liegen — kein Datum um die Zeitumstellung wählen:

```java
"2026-06-15T10:00:00Z"  // sicher CEST → 12:00 Uhr Berlin
"2026-01-15T11:00:00Z"  // sicher CET  → 12:00 Uhr Berlin
```

---

## Account-Linking-Lambda testen

`AccountLinkingHandler` hat einen package-privaten Konstruktor mit Umgebungs-Map. Events im API-Gateway-Format (Payload 2.0) liefert `ApiGatewayEvents`:

```java
handler = new AccountLinkingHandler(Map.of(
        LinkingSettings.ENV_CLIENT_ID, "tibber-skill-client",
        LinkingSettings.ENV_REDIRECT_URIS, "https://layla.amazon.com/api/skill/link/M2ABCDEFGHIJ",
        SkillSettings.ENV_TIBBER_API_URL, tibber.uri().toString()));

JsonNode response = invoke(post("/link", form(fields), HOST, "https://" + HOST));
assertThat(response.path("headers").path("Location").asText()).startsWith(REDIRECT + "#state=");
```

Dass der Token nie geloggt wird, prüft `post_neverLogsOrEchoesToken` mit einem Logback-`ListAppender` am Root-Logger.
