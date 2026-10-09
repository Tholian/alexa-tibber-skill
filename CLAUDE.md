# Alexa Tibber Skill – Claude Context

## Tech Stack

- **AWS Lambda** (Runtime `java25`, arm64, SnapStart) – Einstieg `TibberSkillStreamHandler`
- **Java 25** (JDK toolchain), Anwendungscode in Java
- **Kotlin DSL** ausschließlich für Gradle-Dateien (`.gradle.kts`), **Gradle 9.4**
- **ASK SDK 2.86.0** (`ask-sdk-core` + `ask-sdk-lambda-support`)
- **java.net.http.HttpClient** + Jackson 2 (`jackson-databind` 2.18.3 explizit deklariert; log4j aus dem ASK SDK wird per Exclude entfernt) für die Tibber GraphQL API
- **JUnit 6**, AssertJ, Mockito
- **Lombok** nur für `@Slf4j`
- **JSpecify** – `@NullMarked` je Package, `@Nullable` explizit
- **Logback + Logstash Logback Encoder 8.1** – JSON-Logs nach CloudWatch
- **AWS SAM** (`template.yaml`) für das Deployment

## Abhängigkeits-Regeln

- **ASK SDK:** nur die zwei Module `com.amazon.alexa:ask-sdk-core:2.86.0` und `com.amazon.alexa:ask-sdk-lambda-support:2.86.0` (enthält `SkillStreamHandler`), nicht das Aggregat `ask-sdk` (zieht unnötige Zusatzmodule ins Lambda-Paket).
- **`commons-io`** ist per Gradle-`constraints {}` auf 2.18.0 festgelegt (transitives 2.7 aus `ask-sdk-lambda-support` hat CVE-2021-29425).
- **Kein DI- oder Web-Framework.** Verdrahtung von Hand in `config/SkillFactory` (`new CustomSkillBuilder()`). Neue Handler dort in `requestHandlers(...)` eintragen.
- **HTTP:** `java.net.http.HttpClient` aus dem JDK, keine zusätzliche HTTP-Bibliothek.
- **Lombok:** nur `compileOnly` + `annotationProcessor`; ausschließlich `@Slf4j` (`lombok.config` erzwingt das per Compile-Fehler).
- **JetBrains Annotations:** `compileOnly` – für `@Language("GraphQL")` in `TibberService`.
- Jede neue Laufzeitabhängigkeit vergrößert Lambda-Paket und Kaltstart – vorher abwägen.
- **Lambda-Paket:** Zip = `sourceSets.main.output` (Klassen + `logback.xml`) im Root, Laufzeitabhängigkeiten unter `lib/`.

## Projektstruktur

```
.github/workflows/ci.yml                            # GitHub Actions: build + Tests, Lambda-Zip als Artefakt (kein Deployment)
template.yaml                                       # AWS SAM (Lambda, Alias "live", Alexa-Trigger, Log-Retention)
skill-package/interactionModels/custom/de-DE.json   # Interaction Model (deutsche Utterances)

src/main/java/org/tholian/alexa/tibber/
├── TibberSkillStreamHandler.java     # Lambda-Einstieg (::handleRequest)
├── config/
│   ├── SkillFactory.java             # manuelle Verdrahtung aller Komponenten
│   ├── SkillSettings.java            # ALEXA_SKILL_ID (Pflicht), TIBBER_API_URL (optional; nur https, http nur Loopback – sonst Init-Fehler)
│   └── TibberApiUrls.java            # gemeinsame Validierung der Tibber-URL
├── handler/                          # 9 Request-Handler, Catch-all UnhandledRequestHandler, 2 Interceptoren, GenericExceptionHandler,
│                                     # AccountLinkingResponses (Link-/Relink-Antworten, Token-Auslese)
├── linking/                          # Account-Linking-Brücke (Implicit Grant): AccountLinkingHandler (Lambda-Einstieg),
│                                     # AuthorizationRequest, LinkingSettings, LinkingPage, FormData, HttpResult,
│                                     # InvalidAuthorizationRequestException
├── model/                            # Records für die Tibber GraphQL API
└── service/
    ├── TibberApiException.java
    ├── TibberAuthenticationException.java
    └── TibberService.java            # Tibber-Aufrufe + Formatierung

src/main/resources/logback.xml        # JSON (LogstashEncoder)

src/test/java/org/tholian/alexa/tibber/
├── TibberSkillStreamHandlerTest.java # End-to-End: JSON rein, JSON raus
├── config/                           # SkillSettingsTest, SkillFactoryTest, InteractionModelCoverageTest
├── handler/                          # Handler-Tests + HandlerTestHelper, AccountLinkingResponsesTest
├── integration/TibberServiceIntegrationTest.java   # nur mit TIBBER_DEMO_TOKEN (sendet Daten an Tibber)
├── linking/                          # AccountLinkingHandlerTest (End-to-End inkl. Token-Leck-Prüfung), AuthorizationRequestTest,
│                                     # FormDataTest, HttpResultTest, LinkingPageTest, LinkingSettingsTest
├── service/TibberServiceTest.java
└── testsupport/                      # FakeTibberServer, TibberJson, AlexaRequestJson, ApiGatewayEvents
src/test/resources/logback-test.xml   # Plaintext für Tests
```

## Key Design Decisions

- **Lambda-Einstieg:** öffentlicher No-Arg-Konstruktor liest `System.getenv()`, package-privater Konstruktor mit `Map` für Tests. Der Skill wird einmal pro Execution Environment gebaut (SnapStart).
- **Absicherung:** Alexa-Trigger der Lambda (Resource Policy auf die Skill-ID) + `withSkillId(...)` im ASK SDK. Eine Request-Signaturprüfung ist nur bei HTTPS-Endpoints nötig, nicht bei Lambda.
- **Fail-fast:** ohne `ALEXA_SKILL_ID` schlägt der Lambda-Init fehl.
- **Timeouts:** Tibber Connect 2 s, Request 4 s als Gesamt-Timeout inkl. Body (`sendAsync` + `future.get`; Alexa bricht nach 8 s ab). Fehler → `TibberApiException` (Message ohne Token) → Handler sprechen eine Fehlermeldung.
- **Tibber Token via Alexa Account Linking** – aus `context.System.user.accessToken`.
- **Account Linking:** Implicit Grant über die eigene Lambda `AccountLinkingHandler` hinter API Gateway HTTP API (`/link`; je Route GET/POST 5 req/s, Burst 10). Tibber bietet für die Preis-API kein OAuth, deshalb fügen Nutzer ihren persönlichen Token ein. Die Seite prüft ihn per Minimalabfrage (`verifyToken`) und leitet nur an exakt konfigurierte Amazon-Redirect-URLs weiter. Kein Speicher, kein Logging des Tokens, kein JavaScript, strenge CSP.
- **Abgelehnter Token** (HTTP 401/403 oder GraphQL `UNAUTHENTICATED`) → `TibberAuthenticationException` → Handler antworten über `AccountLinkingResponses.relinkAccount` mit LinkAccount-Card.
- **Linking-Seite – Fallstricke:**
  - `Referrer-Policy: same-origin`, **nicht** `no-referrer`: Bei `no-referrer` senden Browser beim Formular-POST `Origin: null`, und die Herkunftsprüfung (`Origin` == `https://` + `Host`) lehnt jede echte Verknüpfung ab.
  - GET-Parameter werden aus `rawQueryString` mit `FormData.parse` gelesen (strikt, doppelte Parameter → 400, `+` = Leerzeichen laut RFC 6749), nicht aus `queryStringParameters` (API Gateway fasst Duplikate kommagetrennt zusammen).
  - Die API läuft auf der Stage `$default`; der Handler prüft `rawPath == "/link"` und das Formular postet auf `/link` – keine benannte Stage einführen.
  - Unerwartete Exceptions fängt `handle` ab und loggt nur den Klassennamen (Messages könnten Eingaben enthalten); `HttpResult.toString()` schwärzt `Location`, weil dort der Token steht.
  - Keine `script-src` in der CSP: Die Seite kommt ohne JavaScript aus; das einzige Inline-Style ist per SHA-256-Hash erlaubt – Änderungen an `LinkingPage.CSS` ändern den Hash automatisch.
- **Preise in Cent**, **Timezone Europe/Berlin**, Preisliste = `today` + `tomorrow`; `getNextLowestPrice`/`getNextHighestPrice` filtern auf Zukunft über die injizierte `Clock`.
- **`UnhandledRequestHandler`** ist ein Catch-all und wird in `SkillFactory.create` als LETZTER Handler registriert: ASK SDK 2.86.0 liefert bei fehlendem passendem Handler `null` (keine Exception) – Exception-Handler sehen unbekannte Intents nie. `GenericExceptionHandler` behandelt ausschließlich geworfene Exceptions und antwortet gesprochen statt mit Lambda-Fehler.
- **`SkillFactory.requestHandlers(...)`** enthält bewusst nur die 9 spezifischen Handler (ohne Catch-all), damit `InteractionModelCoverageTest` aussagekräftig bleibt: Er stellt sicher, dass jeder Intent aus dem Interaction Model einen Handler hat.
- **Tibber `Consumption`-Typ:** gültige Felder `from`, `to`, `consumption`, `cost` (`unit` existiert nicht → `GRAPHQL_VALIDATION_FAILED`).

## Logging

- Produktion: JSON via `LogstashEncoder` (`logback.xml`) → CloudWatch, Retention 14 Tage (SAM); Log-Gruppen `/aws/lambda/<Stack-Name>` (Skill) und `/aws/lambda/<Stack-Name>-account-linking` (Linking-Seite), jeweils per `LoggingConfig` an die Funktion gebunden.
- Tests: Plaintext mit MDC-Feldern (`logback-test.xml`).
- MDC wird zu Beginn jedes Requests geleert (`LoggingInterceptor`) und danach (`MDCCleanupInterceptor`) – wichtig für warme Lambda-Instanzen.

| MDC-Feld | Quelle |
|---|---|
| `intentName` | `IntentRequest.getIntent().getName()` oder Klassenname des Request-Typs |
| `requestId` | `request.getRequestId()` |
| `locale` | `request.getLocale()` |
| `userId` | `context.System.user.userId` (pseudonyme Alexa-ID) |

**Sicherheit:** `accessToken` (Tibber Bearer Token) darf **niemals** geloggt werden.

## Tests

- Tibber-HTTP wird nicht gemockt: `FakeTibberServer` (JDK `HttpServer`, Zufallsport, Loopback) + `TibberJson`-Fixtures.
- Alexa-Requests als JSON: `AlexaRequestJson`.
- Handler-Tests: `HandlerTestHelper` + Mockito-Mock von `TibberService`.
- Linking-Lambda: Events im API-Gateway-Format (Payload 2.0) über `ApiGatewayEvents`; `post_neverLogsOrEchoesToken` prüft per Logback-`ListAppender`, dass der Token nie geloggt wird.
- Details → `SKILLS.md`

## Code-Stil

- Main-Code: Tabs, Leerzeichen in Klammern (`foo( x )`, `bar( )`); Test-Code: 4 Spaces, ohne Klammer-Leerzeichen.
- Kommentare auf Deutsch mit Umlauten; KI-generierte Stellen mit Präfix `KI-generiert:`, sicherheitsrelevante zusätzlich `SICHERHEIT:`.
- Annotationen alphabetisch nach Simple Name.
- Commits im Conventional-Commits-Format (`type(scope): subject`).

## Skill-Konfiguration

- Invocation Name: **"tibber strompreis"**
- Endpoint: Lambda-Alias `live` (Stack-Output `SkillEndpointArn`), Region `eu-west-1`
- Account Linking: Implicit Grant, Authorization URI = Stack-Output `AccountLinkingAuthorizationUri`, Parameter `LinkingClientId`, `LinkingRedirectUris`, `PrivacyPolicyUrl`.
- Demo-Token: im Tibber Developer Portal; nur als Umgebungsvariable `TIBBER_DEMO_TOKEN` verwenden, nie einchecken. Der Demo-Account liefert Preise in **SEK**.

## Backlog

Offene Aufgaben, technische Schulden und Feature Requests stehen in `BACKLOG.md`. Neue Punkte (auch aus Reviews) dort im passenden Abschnitt aufnehmen; erledigte mit Datum und Commit nach „Erledigt“ verschieben. Keine personenbezogenen Daten oder Tokens eintragen.

## Build & Test

```bash
./gradlew build                                  # Tests + build/distributions/alexa-tibber-skill.zip
./gradlew test --tests "*.TibberServiceTest"
# Integrationstest gegen die echte Tibber API (nur manuell durch den User; --rerun ist nötig, weil Gradle
# die Umgebungsvariable nicht als Eingabe erkennt und sonst ein gecachtes Ergebnis mit übersprungenen Tests liefert)
# Git Bash:
TIBBER_DEMO_TOKEN=<token> ./gradlew test --tests "*.TibberServiceIntegrationTest" --rerun
# PowerShell:
$env:TIBBER_DEMO_TOKEN = '<token>'; .\gradlew.bat test --tests "*.TibberServiceIntegrationTest" --rerun
# Deployment (sam deploy) nur manuell durch den Maintainer – siehe README
```
