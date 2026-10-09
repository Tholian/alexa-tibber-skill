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
    @Mock TibberService tibberService;
    private SomeIntentHandler handler;

    @BeforeEach void setUp() { handler = new SomeIntentHandler(tibberService); }

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
private TibberService service;

@BeforeEach void setUp() {
    server = new FakeTibberServer();
    service = new TibberService(HttpClient.newHttpClient(), new ObjectMapper(), server.uri(),
            Duration.ofMillis(500), Clock.fixed(NOW, ZoneOffset.UTC));
}

@AfterEach void tearDown() { server.close(); }

// Pro Test:
server.respondWith(200, homeResponse(price(0.25, "NORMAL", "2026-06-15T10:00:00Z"), "[]", "null", "[]"));
server.delayResponsesBy(Duration.ofSeconds(2));   // Timeout-Test
assertThat(server.lastAuthorization()).isEqualTo("Bearer test-token");
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
class TibberServiceIntegrationTest { ... }
```

Manueller Aufruf (Demo-Token aus dem Tibber Developer Portal, nie einchecken):
```bash
TIBBER_DEMO_TOKEN=<demo-token> ./gradlew test --tests "*.TibberServiceIntegrationTest"
```

**Tibber GraphQL-Schema Hinweise:**
- `unit` ist **kein** gültiges Feld auf `Consumption` → `GRAPHQL_VALIDATION_FAILED`. Gültige Felder: `from`, `to`, `consumption`, `cost`, `consumptionUnit`
- Demo-Account liefert Preise in **SEK**, nicht EUR — Assertions auf `total` daher mit `isFinite()` statt `isGreaterThan(0.0)` (Spotpreise können 0 oder negativ sein)
- Bei `UNAUTHENTICATED`-Fehler: Demo-Token ist abgelaufen → neuen Token in Tibber API-Dokumentation suchen

---

## Timestamps in Tests

`TibberService` bekommt eine `Clock` injiziert. Für `getNextLowestPrice`/`getNextHighestPrice` die Uhr fest stellen und feste Zeitpunkte davor/danach verwenden:

```java
private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");
Clock.fixed(NOW, ZoneOffset.UTC)
"2026-06-15T09:00:00Z"   // Vergangenheit
"2026-06-15T11:00:00Z"   // Zukunft
```

Für `formatTime`-Tests: Datum muss sicher in CEST (Sommer) oder CET (Winter) liegen — kein Datum um die Zeitumstellung wählen:

```java
"2026-06-15T10:00:00Z"  // sicher CEST → 12:00 Uhr Berlin
"2026-01-15T11:00:00Z"  // sicher CET  → 12:00 Uhr Berlin
```
