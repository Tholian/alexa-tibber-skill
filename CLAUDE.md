# Alexa Tibber Skill – Claude Context

## Tech Stack

- **Spring Boot 4.0.5** + Spring Framework 7
- **Java 25** (JDK toolchain), Anwendungscode in Java
- **Kotlin DSL** ausschließlich für Gradle-Dateien (`.gradle.kts`)
- **Gradle 9.4** mit Kotlin DSL
- **ASK SDK 2.86.0** (Alexa Skills Kit)
- **JUnit 6** via `spring-boot-starter-test`
- **Spring RestClient** für Tibber GraphQL API
- **Lombok** für `@Slf4j`-Annotation (nur Logging, kein weiterer Einsatz)
- **Logstash Logback Encoder 8.1** für strukturiertes JSON-Logging (CloudWatch)

## Abhängigkeits-Regeln

**ASK SDK:** `ask-sdk:2.86.0` verwenden mit Exclusion von `ask-sdk-servlet-support` (javax.servlet kollidiert mit Jakarta EE 11). Nicht `ask-sdk-core` allein.

```kotlin
implementation("com.amazon.alexa:ask-sdk:2.86.0") {
    exclude(group = "com.amazon.alexa", module = "ask-sdk-servlet-support")
}
```

**Kein** `spring-boot-starter-graphql` — server-seitiger Starter, nicht für Client-Anfragen. `RestClient` aus `spring-boot-starter-web` reicht.

**Lombok:** Nur als `compileOnly` + `annotationProcessor` (kein Runtime-Dependency). Ausschließlich `@Slf4j` verwenden — alle anderen Logger-Annotationen sind in `lombok.config` auf `error` gesetzt.

**JetBrains Annotations:** `compileOnly("org.jetbrains:annotations:24.1.0")` — wird für `@Language("GraphQL")` in `TibberService` benötigt (IDE-Syntax-Highlighting für eingebettete GraphQL-Strings).

## Projektstruktur

```
src/main/java/org/tholian/alexa/tibber/
├── AlexaTibberSkillApplication.java
├── config/SkillConfig.java          # Skill-Bean + RestClient-Bean + Interceptor-Registrierung
├── controller/SkillController.java  # POST /alexa
├── handler/                         # 9 Alexa-Request-Handler + 2 Interceptors
│   ├── LaunchRequestHandler.java
│   ├── CurrentPriceIntentHandler.java       # GetCurrentPriceIntent
│   ├── CurrentConsumptionIntentHandler.java # GetCurrentConsumptionIntent
│   ├── LowestPriceIntentHandler.java        # GetLowestPriceIntent
│   ├── HighestPriceIntentHandler.java       # GetHighestPriceIntent
│   ├── HelpIntentHandler.java
│   ├── FallbackIntentHandler.java
│   ├── CancelAndStopIntentHandler.java
│   ├── SessionEndedRequestHandler.java
│   ├── LoggingInterceptor.java      # RequestInterceptor: MDC befüllen vor jedem Request
│   └── MDCCleanupInterceptor.java   # ResponseInterceptor: MDC leeren nach jedem Request
├── model/
│   ├── GraphQLRequest.java          # record GraphQLRequest(String query)
│   ├── Price.java                   # record: total, currency, level, startsAt
│   ├── ConsumptionNode.java         # record: from, to, consumption, unit, cost
│   └── TibberResponse.java          # verschachtelte Records für GraphQL-Antwort
└── service/TibberService.java       # Tibber API-Aufrufe + Formatierung

src/main/resources/
├── application.yml                  # server.port=8080, tibber.api-url, spring.profiles.active=local
├── logback-spring.xml               # Profil 'local': Plaintext; alle anderen: JSON (LogstashEncoder)
└── interactionModel.json            # Alexa Intent-Schema (deutsche Utterances)

src/test/java/org/tholian/alexa/tibber/
├── service/TibberServiceTest.java          # 19 Tests, RestClient gemockt
├── handler/
│   ├── HandlerTestHelper.java             # ASK SDK Builder-Hilfen, extractSpeech()
│   ├── LoggingInterceptorTest.java        # 6 Tests: MDC-Felder + Null-Safety
│   ├── MDCCleanupInterceptorTest.java     # 1 Test: MDC.clear()
│   ├── CurrentPriceIntentHandlerTest.java
│   ├── CurrentConsumptionIntentHandlerTest.java
│   ├── LowestPriceIntentHandlerTest.java
│   ├── HighestPriceIntentHandlerTest.java
│   ├── LaunchRequestHandlerTest.java
│   ├── HelpIntentHandlerTest.java
│   ├── FallbackIntentHandlerTest.java
│   ├── CancelAndStopIntentHandlerTest.java
│   └── SessionEndedRequestHandlerTest.java
├── controller/SkillControllerTest.java    # MockMvc standaloneSetup
└── integration/TibberServiceIntegrationTest.java  # @Disabled, manuell
```

## Key Design Decisions

- **Kein `ask-sdk-servlet-support`** – `SkillController` deserialisiert manuell mit `ObjectMapperFactory.getMapper()` aus dem ASK SDK
- **Tibber Token via Alexa Account Linking** – Token kommt aus `context.system.user.accessToken`
- **Preise in Cent** – Tibber liefert Euro, Ausgabe an Alexa in Cent
- **Timezone Europe/Berlin** für alle Zeitformatierungen
- **Preisliste** = `today` + `tomorrow` aus der Tibber API kombiniert (für 24h-Lookups)
- **`getNextLowestPrice` / `getNextHighestPrice`** filtern auf Preise, deren `startsAt` in der Zukunft liegt
- **TODO Production**: Alexa Request-Signaturverifizierung (aktuell deaktiviert)
- **`ConsumptionNode.unit`** – Feld existiert im Model, wird aber nie befüllt: `unit` ist kein gültiges Feld auf dem Tibber `Consumption`-Typ (GraphQL-Fehler `GRAPHQL_VALIDATION_FAILED`). Gültige Felder: `from`, `to`, `consumption`, `cost` (und `consumptionUnit` falls benötigt)

## Logging

### Konzept

Strukturiertes JSON-Logging für CloudWatch. Jeder Log-Eintrag enthält Alexa-Request-Kontext als eigene Felder. Lokal bleibt Plaintext.

### Profile

| Profil | Format | Aktivierung |
|--------|--------|-------------|
| `local` (default) | Plaintext mit MDC-Feldern im Pattern | `spring.profiles.active=local` in `application.yml` |
| alles andere (`prod`, `staging`, …) | JSON via `LogstashEncoder` | Env-Variable: `SPRING_PROFILES_ACTIVE=prod` |

### MDC-Felder (automatisch in jedem Log-Eintrag)

| Feld | Quelle | Null-safe? |
|------|--------|------------|
| `intentName` | `IntentRequest.getIntent().getName()` oder Klassenname des Request-Typs | ja |
| `requestId` | `request.getRequestId()` | ja |
| `locale` | `request.getLocale()` | ja |
| `userId` | `context.getSystem().getUser().getUserId()` | ja (opaque Alexa ID, kein PII) |

**Sicherheit:** `accessToken` (Tibber Bearer Token) darf **niemals** geloggt werden.

### Logger-Deklaration

Alle Klassen verwenden `@Slf4j` (Lombok). Manuelle `LoggerFactory.getLogger()`-Aufrufe sind nicht mehr nötig:

```java
@Slf4j
@Component
public class MyHandler implements RequestHandler {
    // log.info(...), log.error(...) etc. direkt nutzbar
}
```

### Interceptor-Registrierung in SkillConfig

```java
Skills.custom()
    .addRequestHandlers(...)
    .addRequestInterceptors(loggingInterceptor)
    .addResponseInterceptors(mdcCleanupInterceptor)
    .build();
```

## Spring Boot 4 Test-Besonderheiten

`@WebMvcTest` und `@MockBean` existieren in Spring Boot 4 **nicht mehr**. Controller-Tests verwenden stattdessen:

```java
@ExtendWith(MockitoExtension.class)
class SkillControllerTest {
    @Mock CustomSkill skill;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SkillController(skill))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
```

Details und weitere Test-Patterns → `SKILLS.md`

## Skill-Konfiguration

- Invocation Name: **"tibber strompreis"**
- Endpoint: `POST /alexa` (Port 8080)
- Demo-Token (Tibber Entwicklungs-Daten, Stand April 2026): `3A77EECF61BD445F47241A5A36202185C35AF3AF58609E19B53F3A8872AD7BE1-1`
- Demo-Account liefert Preise in **SEK** (schwedische Krone), nicht EUR
- Alter Token `5K4MVS-OjfWhK_4yrjOlFe1F6kJXPVf7eQYggo8ebAE` ist abgelaufen (UNAUTHENTICATED)

## Build & Test

```bash
./gradlew build
./gradlew bootRun
./gradlew test                          # alle Tests (IntegrationTest übersprungen)
./gradlew test --tests "*.TibberServiceTest"

# Integrationstest manuell gegen echte Tibber API:
./gradlew test --tests "*.TibberServiceIntegrationTest" \
  -Djunit.jupiter.conditions.deactivate="org.junit.jupiter.api.condition.DisabledCondition"

# Lokales Testen mit HTTPS:
ngrok http 8080

# Produktion: JSON-Logging aktivieren
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```
