# Contributing to alexa-tibber-skill

Danke, dass du zu diesem Projekt beitragen möchtest! 🎉  
Jeder Beitrag ist willkommen – egal ob Bugfix, neues Feature, Dokumentation oder einfach eine Frage.

---

## Inhaltsverzeichnis

- [Verhaltenskodex](#verhaltenskodex)
- [Wie kann ich beitragen?](#wie-kann-ich-beitragen)
- [Entwicklungsumgebung einrichten](#entwicklungsumgebung-einrichten)
- [Branching-Strategie](#branching-strategie)
- [Commit-Konventionen](#commit-konventionen)
- [Pull Request Prozess](#pull-request-prozess)
- [Code-Stil](#code-stil)

---

## Verhaltenskodex

Dieses Projekt folgt dem [Contributor Covenant](https://www.contributor-covenant.org/de/version/2/1/code_of_conduct/).  
Bitte geh respektvoll miteinander um – konstruktive Kritik ist immer willkommen, persönliche Angriffe nicht.

---

## Wie kann ich beitragen?

### 🐛 Einen Bug melden

1. Prüfe zuerst, ob der Bug bereits als [Issue](../../issues) gemeldet wurde.
2. Falls nicht, öffne ein neues Issue mit:
    - einer klaren Beschreibung des Problems
    - Schritten zur Reproduktion
    - erwartetem vs. tatsächlichem Verhalten
    - Logs oder Screenshots (falls hilfreich)
    - deiner Umgebung (Java-Version, OS, Alexa-Region)

### 💡 Ein Feature vorschlagen

Öffne ein Issue mit dem Label `enhancement` und beschreibe:

- Was soll der Skill können, das er noch nicht kann?
- Warum ist das für andere Nutzer nützlich?
- Hast du schon eine Idee zur Umsetzung?

Angenommene Vorschläge werden in die [BACKLOG.md](BACKLOG.md) unter „Feature Requests“ übernommen.

### 🔧 Code beisteuern

Schau in die offenen Issues – besonders solche mit dem Label `good first issue` eignen sich für den Einstieg.  
Für größere Änderungen bitte vorher ein Issue öffnen, um die Umsetzung abzustimmen.

---

## Entwicklungsumgebung einrichten

### Voraussetzungen

- **Java 25** (z. B. via [SDKMAN](https://sdkman.io/): `sdk install java 25-open`)
- **Gradle** (wird über den Wrapper `./gradlew` automatisch heruntergeladen)
- Optional: **AWS SAM CLI** – nur für Maintainer, die deployen

### Setup

```bash
# 1. Repository forken und klonen
git clone https://github.com/DEIN-USERNAME/alexa-tibber-skill.git
cd alexa-tibber-skill

# 2. Bauen und alle Tests ausführen (erzeugt auch das Lambda-Paket)
./gradlew build
```

### Lokal testen

Die Tests decken den kompletten Weg ab: `TibberSkillStreamHandlerTest` schickt echte Alexa-Request-JSONs durch den Lambda-Einstieg, die Tibber API wird durch einen lokalen Test-Server (`FakeTibberServer`) ersetzt. Für Sprachtests mit echter Alexa wird der Skill deployt (siehe README) und im Alexa-Simulator getestet.

---

## Branching-Strategie

| Branch                       | Zweck                                 |
|------------------------------|---------------------------------------|
| `main`                       | Stabiler, releasefähiger Stand        |
| `develop`                    | Integrations-Branch für neue Features |
| `feature/kurze-beschreibung` | Neue Features                         |
| `fix/kurze-beschreibung`     | Bugfixes                              |
| `docs/kurze-beschreibung`    | Nur Dokumentation                     |

Bitte erstelle deinen Branch immer von `develop` aus:

```bash
git checkout develop
git pull origin develop
git checkout -b feature/mein-neues-feature
```

---

## Commit-Konventionen

Dieses Projekt verwendet [Conventional Commits](https://www.conventionalcommits.org/de/):

```
<typ>: <kurze Beschreibung im Präsens>

[optionaler längerer Text]

[optionale Referenz auf Issue: Closes #42]
```

### Erlaubte Typen

| Typ        | Bedeutung                          |
|------------|------------------------------------|
| `feat`     | Neues Feature                      |
| `fix`      | Bugfix                             |
| `docs`     | Nur Dokumentation                  |
| `refactor` | Refactoring ohne Funktionsänderung |
| `test`     | Tests hinzufügen oder korrigieren  |
| `chore`    | Build, Dependencies, CI            |
| `perf`     | Performance-Verbesserung           |

### Beispiele

```bash
git commit -m "feat: günstigste Stunde für morgen hinzufügen"
git commit -m "fix: Zeitzone bei Preisformatierung korrekt setzen"
git commit -m "docs: Deployment-Anleitung für Raspberry Pi ergänzen"
git commit -m "chore: ASK SDK auf 2.89.0 aktualisieren"
```

---

## Pull Request Prozess

1. **Branch aktuell halten** – rebase auf `develop` vor dem PR:
   ```bash
   git fetch origin
   git rebase origin/develop
   ```

2. **Tests grün halten** – alle bestehenden Tests müssen bestehen:
   ```bash
   ./gradlew test
   ```

3. **PR öffnen** gegen den `develop`-Branch (nicht `main`) mit:
    - aussagekräftigem Titel (nach Conventional Commits)
    - Beschreibung: Was wurde geändert? Warum?
    - Referenz auf das zugehörige Issue (`Closes #42`)

4. **Review abwarten** – mindestens ein Approval wird benötigt.

5. **Merge** erfolgt per **Squash and Merge** für eine saubere Git-History.

---

## Code-Stil

- **Java 25** – moderne Features nutzen: Records, Text Blocks, Pattern Matching, Switch Expressions
- **Lombok** – ausschließlich `@Slf4j` für Logger. Keine anderen Lombok-Annotationen (`@Data`, `@Builder` etc.) — expliziter Code bleibt verständlicher. `lombok.config` erzwingt das durch Compile-Fehler.
- **Kein Framework für Dependency Injection** – Abhängigkeiten werden per Konstruktor übergeben und in `config/SkillFactory` verdrahtet. Neue Handler dort in `requestHandlers(...)` eintragen; `InteractionModelCoverageTest` prüft, dass jeder Intent einen Handler hat.
- **Null-Safety** – JSpecify: `@NullMarked` je Package (`package-info.java`), nullable Typen explizit mit `@Nullable`.
- **Logging** – `@Slf4j` (Lombok) verwenden. Kein `System.out.println`, keine manuellen `LoggerFactory.getLogger()`-Aufrufe. `accessToken` darf **niemals** geloggt werden.
- **Logging-Format** – Produktion: JSON (`logback.xml`); Tests: Plaintext (`logback-test.xml`).
- **Javadoc** – öffentliche Klassen und Methoden kommentieren
- **Testabdeckung** – neue Features bitte mit Unit-Tests absichern

Formatierung: Hauptcode mit Tabs und Leerzeichen innerhalb von Klammern (`foo( x )`, `bar( )`); Tests mit 4 Leerzeichen und ohne Klammer-Leerzeichen. Kommentare auf Deutsch (mit korrekten Umlauten); von einer KI geschriebene Kommentare und Javadoc beginnen mit `KI-generiert:`. Annotationen alphabetisch sortieren.

---

## Fragen?

Öffne einfach ein [Issue](../../issues) mit dem Label `question` – wir helfen gerne! 😊
