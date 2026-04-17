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

### 🔧 Code beisteuern

Schau in die offenen Issues – besonders solche mit dem Label `good first issue` eignen sich für den Einstieg.  
Für größere Änderungen bitte vorher ein Issue öffnen, um die Umsetzung abzustimmen.

---

## Entwicklungsumgebung einrichten

### Voraussetzungen

- **Java 21** (z. B. via [SDKMAN](https://sdkman.io/): `sdk install java 21-tem`)
- **Gradle** (wird über den Wrapper `./gradlew` automatisch heruntergeladen)
- Ein **Tibber API Token** (Demo-Token zum Testen: `5K4MVS-OjfWhK_4yrjOlFe1F6kJXPVf7eQYggo8ebAE`)
- Optional: Docker für Integrationstests

### Setup

```bash
# 1. Repository forken und klonen
git clone https://github.com/DEIN-USERNAME/alexa-tibber-skill.git
cd alexa-tibber-skill

# 2. Umgebungsvariable setzen (Demo-Token reicht für Entwicklung)
export TIBBER_API_TOKEN=5K4MVS-OjfWhK_4yrjOlFe1F6kJXPVf7eQYggo8ebAE

# 3. Abhängigkeiten laden und bauen
./gradlew build

# 4. Tests ausführen
./gradlew test

# 5. Anwendung starten
./gradlew bootRun
```

### Lokales Testen mit ngrok

Alexa benötigt einen HTTPS-Endpunkt. Für lokale Entwicklung eignet sich [ngrok](https://ngrok.com/):

```bash
# In einem separaten Terminal:
ngrok http 8080

# Die generierte HTTPS-URL in der Alexa Developer Console eintragen
# Außerdem für lokale Entwicklung setzen:
export ALEXA_DISABLE_SIGNATURE_CHECK=true
```

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

- **Java 21** – gerne moderne Features nutzen: Records, Text Blocks, Pattern Matching, Switch Expressions
- **Kein Lombok** – expliziter Code ist verständlicher als Magie
- **Spring-Konventionen** – Constructor Injection statt Field Injection (`@Autowired`)
- **Logging** – `org.slf4j.Logger` verwenden, kein `System.out.println`
- **Javadoc** – öffentliche Klassen und Methoden kommentieren
- **Testabdeckung** – neue Features bitte mit Unit-Tests absichern

Formatierung: Der Code folgt dem Standard-IntelliJ-Codestyle für Java. Bitte vor dem Commit formatieren (`Code → Reformat Code` in IntelliJ, oder `./gradlew spotlessApply` sobald konfiguriert).

---

## Fragen?

Öffne einfach ein [Issue](../../issues) mit dem Label `question` – wir helfen gerne! 😊
