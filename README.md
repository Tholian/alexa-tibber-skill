# Alexa Tibber Skill

Ein **Amazon Alexa Skill** für dynamische Strompreise via [Tibber](https://tibber.com/de) – entwickelt mit **Spring Boot 4**, **Java 25** und dem **ASK SDK**.

Frag Alexa einfach nach dem aktuellen Strompreis, deinem Verbrauch oder der günstigsten bzw. teuersten Stunde des Tages – direkt aus deinem Tibber-Konto.

---

## Features

| Sprachbefehl (Beispiel) | Funktion |
|---|---|
| „Alexa, frag tibber strompreis, was kostet Strom gerade" | Aktueller Strompreis in Cent/kWh |
| „Alexa, frag tibber strompreis, wie viel Strom verbrauche ich gerade" | Aktueller Verbrauch aus dem Smart Meter |
| „Alexa, frag tibber strompreis, wann ist Strom am günstigsten" | Nächste günstigste Stunde (heute/morgen) |
| „Alexa, frag tibber strompreis, wann ist Strom am teuersten" | Nächste teuerste Stunde (heute/morgen) |

**Invocation Name:** `tibber strompreis`

---

## Tech Stack

| Komponente | Version |
|---|---|
| Java | 25 |
| Spring Boot | 4.0.5 |
| Spring Framework | 7 |
| Gradle (Kotlin DSL) | 9.4 |
| Alexa Skills Kit (ASK SDK) | 2.86.0 |
| Logstash Logback Encoder | 8.1 |

---

## Voraussetzungen

- **Java 25** (z. B. via [SDKMAN](https://sdkman.io/): `sdk install java 25-open`)
- **Tibber-Konto** mit aktiviertem [Tibber Pulse](https://tibber.com/de/pulse) oder Smart Meter
- **Tibber API Token** – erhältlich im [Tibber Developer Portal](https://developer.tibber.com/)
- **Amazon Developer Account** für die Alexa Developer Console

---

## Schnellstart

```bash
# Repository klonen
git clone https://github.com/tholian/alexa-tibber-skill.git
cd alexa-tibber-skill

# Tibber API Token setzen (Demo-Token für Entwicklung)
export TIBBER_API_TOKEN=3A77EECF61BD445F47241A5A36202185C35AF3AF58609E19B53F3A8872AD7BE1-1

# Bauen
./gradlew build

# Starten
./gradlew bootRun
```

Der Skill lauscht auf `POST http://localhost:8080/alexa`.

### Lokales Testen mit ngrok

Alexa benötigt einen öffentlich erreichbaren HTTPS-Endpunkt:

```bash
# In einem separaten Terminal:
ngrok http 8080

# Signaturprüfung für lokale Entwicklung deaktivieren:
export ALEXA_DISABLE_SIGNATURE_CHECK=true
```

Die von ngrok generierte `https://`-URL trägst du in der **Alexa Developer Console** unter *Endpoint* ein.

### Alle Tests ausführen

```bash
./gradlew test
```

---

## Alexa Developer Console – Setup

1. Neuen Skill anlegen (Custom, selbst gehostet)
2. Interaction Model aus `src/main/resources/interactionModel.json` importieren
3. Endpoint: HTTPS → deine ngrok- oder Server-URL
4. **Account Linking** aktivieren und mit deinem Tibber OAuth einrichten
5. Skill testen im Alexa Simulator

---

## Architektur

```
Alexa → POST /alexa → SkillController
                         │
                         ├── LoggingInterceptor   (MDC befüllen)
                         ├── [Intent]Handler      (Fachlogik)
                         │       └── TibberService (GraphQL via RestClient)
                         └── MDCCleanupInterceptor (MDC leeren)
```

- **Keine Servlet-Abhängigkeiten** – der Skill läuft nativ auf Jakarta EE 11 / Spring Boot 4, ohne `ask-sdk-servlet-support`
- **Strukturiertes JSON-Logging** für AWS CloudWatch (Profil `prod`), Plaintext lokal (Profil `local`)
- **Tibber Account Linking** – der Bearer Token kommt ausschließlich aus dem Alexa-Kontext, wird nie geloggt

---

## Sicherheitshinweise

> **TODO (Produktion):** Alexa Request-Signaturverifizierung ist aktuell deaktiviert. Vor dem produktiven Einsatz muss die Signaturprüfung aktiviert werden, um sicherzustellen, dass Requests tatsächlich von Amazon stammen.

- Der Tibber API Token wird **nie** in Logs geschrieben
- Keine PII (Personally Identifiable Information) im Logging – nur die opaque Alexa User-ID
- `accessToken` ist ausschließlich zur Laufzeit im Speicher

---

## KI-Unterstützung

Dieses Projekt wurde mit Unterstützung von **Claude (Anthropic)** entwickelt – als Pair-Programming-Partner für Architekturentscheidungen, Code-Reviews und die Umsetzung einzelner Features. KI-generierte oder KI-überarbeitete Codestellen sind im Quellcode mit dem Präfix `KI-generiert:` in Kommentaren gekennzeichnet.

---

## Mitwirken

Beiträge sind herzlich willkommen! Bitte lies zuerst die [CONTRIBUTING.md](CONTRIBUTING.md) – dort findest du:

- Wie du Bugs meldest oder Features vorschlägst
- Wie du die Entwicklungsumgebung einrichtest
- Branching-Strategie und Commit-Konventionen
- Den Pull-Request-Prozess

---

## Lizenz

Dieses Projekt steht unter der [MIT License](LICENSE).

---
