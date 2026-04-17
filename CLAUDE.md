# Alexa Tibber Skill – Claude Context

## Tech Stack

- **Spring Boot 4.0.5** + Spring Framework 7
- **Java 25** (JDK toolchain), Anwendungscode in Java
- **Kotlin DSL** ausschließlich für Gradle-Dateien (`.gradle.kts`)
- **Gradle 9.4** mit Kotlin DSL
- **ASK SDK Core 2.86.0** (Alexa Skills Kit)
- **Spring RestClient** für Tibber GraphQL API

## Wichtige Abhängigkeits-Regel

`ask-sdk-core` verwenden, **NICHT** `ask-sdk`!
`ask-sdk` enthält `ask-sdk-servlet-support` mit einer `javax.servlet`-Abhängigkeit, die mit Spring Boot 4 / Jakarta EE 11 kollidiert.

Außerdem **kein** `spring-boot-starter-graphql` — das ist ein server-seitiger Starter, nicht für Client-Anfragen. Der `RestClient` aus `spring-boot-starter-web` reicht.

## Projektstruktur

```
src/main/java/org/tholian/alexa/tibber/
├── AlexaTibberSkillApplication.java
├── config/SkillConfig.java          # Skill-Bean + RestClient-Bean
├── controller/SkillController.java  # POST /alexa (kein ask-sdk-servlet-support)
├── handler/                         # 9 Alexa-Request-Handler
│   ├── LaunchRequestHandler.java
│   ├── CurrentPriceIntentHandler.java       # GetCurrentPriceIntent
│   ├── CurrentConsumptionIntentHandler.java # GetCurrentConsumptionIntent
│   ├── LowestPriceIntentHandler.java        # GetLowestPriceIntent
│   ├── HighestPriceIntentHandler.java       # GetHighestPriceIntent
│   ├── HelpIntentHandler.java
│   ├── FallbackIntentHandler.java
│   ├── CancelAndStopIntentHandler.java
│   └── SessionEndedRequestHandler.java
├── model/
│   ├── GraphQLRequest.java          # record GraphQLRequest(String query)
│   ├── Price.java                   # record: total, currency, level, startsAt
│   ├── ConsumptionNode.java         # record: from, to, consumption, unit, cost
│   └── TibberResponse.java          # verschachtelte Records für GraphQL-Antwort
└── service/TibberService.java       # Tibber API-Aufrufe + Formatierung

src/main/resources/
├── application.yml
└── interactionModel.json            # Alexa Intent-Schema (deutsche Utterances)
```

## Key Design Decisions

- **Kein `ask-sdk-servlet-support`** – `SkillController` deserialisiert den Request manuell mit `ObjectMapperFactory.getRequestEnvelopeMapper()` aus dem ASK SDK
- **Tibber Token via Alexa Account Linking** – Token kommt aus `context.system.user.accessToken` im Request
- **Preise in Cent** – Tibber liefert Euro, Ausgabe an Alexa in Cent
- **Timezone Europe/Berlin** für alle Zeitformatierungen
- **Preisliste** = `today` + `tomorrow` aus der Tibber API kombiniert (für 24h-Lookups)
- **`getNextLowestPrice` / `getNextHighestPrice`** filtern auf Preise, deren `startsAt` in der Zukunft liegt
- **TODO Production**: Alexa Request-Signaturverifizierung (aktuell deaktiviert)

## Skill-Konfiguration

- Invocation Name: **"tibber strompreis"**
- Endpoint: `POST /alexa` (Port 8080)

## Build & Run

```bash
./gradlew build
./gradlew bootRun

# Lokales Testen mit HTTPS (ngrok):
ngrok http 8080
# → generierte URL in der Alexa Developer Console eintragen
```

## Lokaler Test-Token

Für lokale Entwicklung (Demo-Daten):

```
5K4MVS-OjfWhK_4yrjOlFe1F6kJXPVf7eQYggo8ebAE
```
