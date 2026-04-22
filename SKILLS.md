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

## RestClient mit Mockito mocken

Die Fluent-Chain benötigt zwei separate Mocks — `RequestBodyUriSpec` gibt bei `.header()` einen `RequestBodySpec` zurück:

```java
@Mock RestClient restClient;
@Mock RestClient.RequestBodyUriSpec postSpec;   // restClient.post()
@Mock RestClient.RequestBodySpec bodySpec;       // postSpec.header(...)
@Mock RestClient.ResponseSpec responseSpec;      // bodySpec.retrieve()

@BeforeEach void setUp() {
    // lenient() verhindert UnnecessaryStubbingException in Format-Tests
    lenient().when(restClient.post()).thenReturn(postSpec);
    lenient().when(postSpec.header(anyString(), any(String[].class))).thenReturn(bodySpec);
    lenient().when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
    lenient().when(bodySpec.retrieve()).thenReturn(responseSpec);
    service = new TibberService(restClient);
}

// Pro Test:
when(responseSpec.body(TibberResponse.class)).thenReturn(buildResponse(...));
```

`@MockitoSettings(strictness = Strictness.LENIENT)` auf Klassenebene nötig, da Format-Tests den RestClient-Chain nie aufrufen.

---

## Spring Boot 4: Controller testen

`@WebMvcTest` und `@MockBean` wurden in Spring Boot 4 entfernt. Ersatz für Controller-Tests:

```java
@ExtendWith(MockitoExtension.class)
class SkillControllerTest {

    @RestControllerAdvice
    static class GlobalExceptionHandler {
        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        void handleAll(Exception ex) {}
    }

    @Mock CustomSkill skill;
    private MockMvc mockMvc;

    @BeforeEach void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SkillController(skill))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
```

Hinweis: Spring Framework 7 bietet `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`) als Ersatz für `@MockBean` in Spring-Context-Tests — aber nur wenn ein echter ApplicationContext gebraucht wird.

---

## Integrationstest gegen echte Tibber API

```java
@SpringBootTest
@Disabled("Nur manuell ausführen: erfordert Netzwerkzugang zur Tibber API")
class TibberServiceIntegrationTest {
    // Demo-Token Stand April 2026 — kann sich ändern, bei UNAUTHENTICATED erneuern
    private static final String DEMO_TOKEN = "3A77EECF61BD445F47241A5A36202185C35AF3AF58609E19B53F3A8872AD7BE1-1";
    // ...
}
```

Manueller Aufruf:
```bash
./gradlew test --tests "*.TibberServiceIntegrationTest" \
  -Djunit.jupiter.conditions.deactivate="org.junit.jupiter.api.condition.DisabledCondition"
```

**Tibber GraphQL-Schema Hinweise:**
- `unit` ist **kein** gültiges Feld auf `Consumption` → `GRAPHQL_VALIDATION_FAILED`. Gültige Felder: `from`, `to`, `consumption`, `cost`, `consumptionUnit`
- Demo-Account liefert Preise in **SEK**, nicht EUR — Assertions auf `total` daher mit `isFinite()` statt `isGreaterThan(0.0)` (Spotpreise können 0 oder negativ sein)
- Bei `UNAUTHENTICATED`-Fehler: Demo-Token ist abgelaufen → neuen Token in Tibber API-Dokumentation suchen

---

## Timestamps in Tests

Für `getNextLowestPrice`/`getNextHighestPrice`: relative Timestamps verwenden, keine hardcodierten:

```java
String past   = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1).toString();
String future = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1).toString();
```

Für `formatTime`-Tests: Datum muss sicher in CEST (Sommer) oder CET (Winter) liegen — kein Datum um die Zeitumstellung wählen:

```java
"2026-06-15T10:00:00Z"  // sicher CEST → 12:00 Uhr Berlin
"2026-01-15T11:00:00Z"  // sicher CET  → 12:00 Uhr Berlin
```
