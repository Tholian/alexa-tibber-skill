# Alexa Tibber Skill

Ein **Amazon Alexa Skill** für dynamische Strompreise via [Tibber](https://tibber.com/de) – läuft als **AWS-Lambda-Funktion** mit **Java 25** und dem **ASK SDK**.

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
| AWS Lambda | Runtime `java25`, arm64, SnapStart |
| Alexa Skills Kit (`ask-sdk-core` + `ask-sdk-lambda-support`) | 2.86.0 |
| Gradle (Kotlin DSL) | 9.4 |
| Logback / Logstash Logback Encoder | 1.5 / 8.1 |
| AWS SAM | Deployment |

---

## Voraussetzungen

**Für Entwicklung und Tests:**

- **Java 25** (z. B. via [SDKMAN](https://sdkman.io/): `sdk install java 25-open`)

**Zusätzlich für den Betrieb des Skills** (Einrichtung siehe [Deployment auf AWS](#deployment-auf-aws-schritt-für-schritt)):

- **Tibber-Konto** mit [Tibber Pulse](https://tibber.com/de/pulse) oder Smart Meter
- **Amazon Developer Account** für die [Alexa Developer Console](https://developer.amazon.com/alexa/console/ask)
- **AWS-Konto**
- **AWS CLI v2** und **AWS SAM CLI** auf dem Rechner

---

## Bauen und testen

```bash
git clone https://github.com/tholian/alexa-tibber-skill.git
cd alexa-tibber-skill

# Kompilieren, alle Tests ausführen, Lambda-Paket bauen
./gradlew build
```

Unter Windows PowerShell statt `./gradlew` den Aufruf `.\gradlew.bat` verwenden.

Ergebnis: `build/distributions/alexa-tibber-skill.zip`.

Die Tests spielen komplette Alexa-Requests (JSON) durch den Lambda-Einstieg und ersetzen die Tibber API durch einen lokalen Test-Server – es ist kein Netzwerkzugriff nötig.

### Integrationstest gegen die echte Tibber API

Optional, mit dem öffentlichen Demo-Token aus dem [Tibber Developer Portal](https://developer.tibber.com/):

```bash
TIBBER_DEMO_TOKEN=<demo-token> ./gradlew test --tests "*.TibberServiceIntegrationTest"
```

Ohne gesetzte Variable wird der Test übersprungen. Tokens nie einchecken.

---

## Deployment auf AWS (Schritt für Schritt)

Diese Anleitung setzt keine AWS-Vorkenntnisse voraus. Die einmalige Einrichtung (Teil A) dauert etwa 30–60 Minuten, jedes spätere Update (Teil C) sind nur noch zwei Befehle.

**Was passiert beim Deployment?** `gradlew build` erzeugt `build/distributions/alexa-tibber-skill.zip`. `sam deploy` lädt dieses Paket in einen von SAM verwalteten S3-Bucket in deinem AWS-Konto und legt über AWS CloudFormation alles an, was in `template.yaml` beschrieben ist: die Lambda-Funktion, eine Ausführungsrolle, den Alias `live`, die Berechtigung „nur dieser Alexa Skill darf aufrufen“ und eine Log-Gruppe mit 14 Tagen Aufbewahrung. Dieser Satz Ressourcen heißt **Stack**.

### Was kostet das?

| Dienst | Kosten für einen privat genutzten Skill |
|---|---|
| AWS Lambda | Dauerhaftes Freikontingent von 1 Mio. Aufrufen und 400.000 GB-Sekunden pro Monat – wird praktisch nie überschritten |
| SnapStart (Java) | keine Zusatzkosten |
| CloudWatch Logs | Freikontingent von 5 GB pro Monat; Logs werden nach 14 Tagen gelöscht |
| S3 (Deployment-Pakete) | wenige MB, Cent-Bereich |

Trotzdem empfohlen: einen Budget-Alarm einrichten (Schritt A5), damit Fehlkonfigurationen nicht unbemerkt Geld kosten.

### Teil A – Einmalige Einrichtung

**A1. AWS-Konto anlegen**
Auf [aws.amazon.com](https://aws.amazon.com/) „Kostenloses Konto erstellen“ wählen. Eine Kreditkarte wird benötigt. Die Anmeldung mit E-Mail und Passwort ist der **Root-Benutzer** – er hat uneingeschränkte Rechte über das Konto.

**A2. Root-Benutzer absichern**
In der AWS-Konsole oben rechts auf den Kontonamen → *Sicherheitsanmeldeinformationen* → *MFA zuweisen* (Authenticator-App). Den Root-Benutzer danach nur noch für Kontoverwaltung und Rechnungen verwenden, **nie für Deployments**.

**A3. Arbeitsbenutzer anlegen (IAM Identity Center)**
1. Oben rechts die Region **Europa (Irland) `eu-west-1`** auswählen – Alexa-Skills im deutschsprachigen Raum sollten dort laufen.
2. Den Dienst **IAM Identity Center** öffnen und *Aktivieren* klicken.
3. *Benutzer* → *Benutzer hinzufügen*: Benutzername und E-Mail angeben. Die Einladungsmail annehmen, Passwort und MFA einrichten.
4. *Berechtigungssätze* → *Berechtigungssatz erstellen* → *Vordefiniert* → `AdministratorAccess`. Für ein privates Ein-Personen-Konto ist das am einfachsten; SAM benötigt Rechte für CloudFormation, Lambda, IAM-Rollen, S3 und CloudWatch Logs.
5. *AWS-Konten* → dein Konto auswählen → *Benutzer oder Gruppen zuweisen* → den Benutzer aus Schritt 3 und den Berechtigungssatz aus Schritt 4 zuweisen.
6. Im Dashboard von IAM Identity Center die **URL des AWS-Zugangsportals** notieren (Form `https://d-xxxxxxxxxx.awsapps.com/start`).

**A4. Werkzeuge installieren**
- **AWS CLI v2:** offizieller Installer laut [AWS-Dokumentation](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html). Der Windows-Installer (MSI) benötigt Administratorrechte – auf einem Firmenrechner über die IT bzw. die Softwareverteilung installieren lassen.
- **AWS SAM CLI:** entweder offizieller Installer laut [AWS-Dokumentation](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html) (ebenfalls MSI) oder ohne Administratorrechte in einer eigenen Python-Umgebung:
  ```powershell
  python -m venv $HOME\.venvs\aws-sam
  & $HOME\.venvs\aws-sam\Scripts\Activate.ps1
  pip install --upgrade pip aws-sam-cli
  ```
  Die Umgebung muss in jedem neuen Terminal wieder aktiviert werden (zweite Zeile).
  Schlägt die Aktivierung mit einem Execution-Policy-Fehler fehl, einmalig `Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned` ausführen (keine Administratorrechte nötig; auf verwalteten Firmenrechnern kann das per Richtlinie gesperrt sein – dann die IT fragen). Alternativ das Werkzeug ohne Aktivierung direkt aufrufen: `& $HOME\.venvs\aws-sam\Scripts\sam.exe --version`.
- Prüfen:
  ```powershell
  aws --version
  sam --version
  ```

**A5. Budget-Alarm einrichten**
In der AWS-Konsole *Fakturierung und Kostenmanagement* → *Budgets* → *Budget erstellen* → Vorlage **„Budget ohne Ausgaben“** (Zero spend budget) mit deiner E-Mail-Adresse. Du bekommst dann eine Mail, sobald überhaupt Kosten anfallen.

**A6. AWS CLI anmelden (ohne dauerhafte Zugangsschlüssel)**
```powershell
aws configure sso
```
Antworten:

| Frage | Antwort |
|---|---|
| SSO session name | `alexa` |
| SSO start URL | Zugangsportal-URL aus A3, Schritt 6 |
| SSO region | `eu-west-1` |
| SSO registration scopes | Enter (Standard) |
| *(Browser öffnet sich – Zugriff bestätigen, Konto und Rolle auswählen)* | |
| Default client Region | `eu-west-1` |
| CLI default output format | Enter |
| Profile name | `alexa-tibber` |

Test:
```powershell
aws sts get-caller-identity --profile alexa-tibber
```
Die Ausgabe zeigt Konto-ID und Rolle. Die Anmeldung läuft nach einigen Stunden ab; erneuern mit `aws sso login --profile alexa-tibber`.

> **Sicherheit:** Es werden keine dauerhaften Access Keys angelegt. Die temporären Anmeldetoken liegen unter `%USERPROFILE%\.aws\` – diesen Ordner nie teilen oder einchecken.

**A7. Alexa Skill anlegen**
1. [Alexa Developer Console](https://developer.amazon.com/alexa/console/ask) → *Create Skill*.
2. Name z. B. „Tibber Strompreis“, Primary locale **German (DE)**, Typ **Custom**, Hosting **Provision your own**, Vorlage *Start from scratch*.
3. *Build* → *Interaction Model* → *JSON Editor*: den Inhalt von `skill-package/interactionModels/custom/de-DE.json` einfügen → *Save* → *Build skill*.
4. Die **Skill-ID** kopieren (in der Skill-Liste *Copy Skill ID*; Format `amzn1.ask.skill.<uuid>`).

### Teil B – Erstes Deployment

```powershell
.\gradlew.bat build
sam deploy --guided --profile alexa-tibber
```

Antworten im geführten Modus:

| Frage | Antwort |
|---|---|
| Stack Name | `alexa-tibber-skill` |
| AWS Region | `eu-west-1` |
| Parameter AlexaSkillId | Skill-ID aus A7 |
| Confirm changes before deploy | `y` – SAM zeigt vor jeder Änderung an, was passiert |
| Allow SAM CLI IAM role creation | `y` – die Lambda-Funktion braucht eine Ausführungsrolle |
| Disable rollback | `n` – bei Fehlern wird automatisch zurückgerollt |
| Save arguments to configuration file | `y` – speichert die Antworten in `samconfig.toml` (lokal, nicht eingecheckt) |
| SAM configuration file / environment | Enter (Standard) |

Am Ende zeigt SAM unter **Outputs** den Wert `SkillEndpointArn`, z. B. `arn:aws:lambda:eu-west-1:123456789012:function:alexa-tibber-skill-TibberSkillFunction-AbC123:live`.

**Endpoint im Skill eintragen:**
1. Alexa Developer Console → dein Skill → *Build* → *Endpoint*.
2. **AWS Lambda ARN** wählen und den ARN unter *Default Region* einfügen. Er muss auf `:live` enden.
3. *Save Endpoints*.

**Account Linking einrichten:** *Build* → *Tools* → *Account Linking* mit den OAuth-Daten deines Tibber-Zugangs aus dem [Tibber Developer Portal](https://developer.tibber.com/) konfigurieren. Danach in der Alexa-App den Skill aktivieren und das Tibber-Konto verknüpfen.

**Testen:** Reiter *Test* → *Skill testing is enabled in: Development* → „frag tibber strompreis was kostet strom gerade“.

### Teil C – Updates ausrollen

```powershell
aws sso login --profile alexa-tibber    # nur falls die Anmeldung abgelaufen ist
.\gradlew.bat build
sam deploy --profile alexa-tibber       # nutzt die gespeicherten Antworten aus samconfig.toml
```

Jedes Deployment veröffentlicht eine neue Lambda-Version; SnapStart erstellt dafür einen Snapshot, und der Alias `live` zeigt automatisch darauf. In der Alexa Developer Console muss nichts geändert werden.

### Logs und Fehlersuche

Logs live mitlesen:
```powershell
sam logs --stack-name alexa-tibber-skill --name TibberSkillFunction --tail --profile alexa-tibber
```
Alternativ in der AWS-Konsole unter *CloudWatch* → *Logs Insights*, z. B. nur Fehler:
```
fields @timestamp, level, intentName, message
| filter level = "ERROR"
| sort @timestamp desc
```

| Symptom | Ursache und Lösung |
|---|---|
| `Token has expired` / `ExpiredToken` bei `sam`-Befehlen | Anmeldung abgelaufen → `aws sso login --profile alexa-tibber` |
| Alexa sagt „Beim angeforderten Skill ist ein Problem aufgetreten“ | Logs prüfen. Häufig: Endpoint-ARN endet nicht auf `:live` oder `AlexaSkillId` stimmt nicht mit der Skill-ID überein (Parameter neu setzen: `sam deploy --guided --profile alexa-tibber`) |
| Log-Eintrag „Umgebungsvariable ALEXA_SKILL_ID fehlt“ | Parameter `AlexaSkillId` beim Deployment leer → erneut `sam deploy --guided` |
| `sam deploy` meldet, die Runtime `java25` sei unbekannt | Die Region bietet Java 25 noch nicht an → in `template.yaml` `Runtime: java21` und in `build.gradle.kts` die Toolchain auf 21 setzen |
| Alexa antwortet, das Konto sei nicht verknüpft | Account Linking in der Alexa-App durchführen |

### Wieder entfernen

```powershell
sam delete --stack-name alexa-tibber-skill --profile alexa-tibber
```
Löscht Lambda-Funktion, Rolle, Alias und Log-Gruppe. Anschließend in der Alexa Developer Console den Endpoint entfernen bzw. den Skill löschen.

---

## Architektur

```
Alexa ──(Lambda-Invoke, nur diese Skill-ID)──▶ TibberSkillStreamHandler
                                                 │  SkillFactory verdrahtet alles einmalig beim Init
                                                 ├── LoggingInterceptor       (MDC befüllen)
                                                 ├── [Intent]Handler          (Fachlogik)
                                                 │       └── TibberService    (GraphQL via java.net.http)
                                                 ├── UnhandledRequestHandler  (Catch-all für unbekannte Intents)
                                                 ├── GenericExceptionHandler  (Fallback-Antwort bei Exceptions)
                                                 └── MDCCleanupInterceptor    (MDC leeren)
```

- Kein Web-Server und kein DI-Framework – der Skill wird beim Lambda-Init einmal gebaut und per SnapStart gesichert.
- Strukturiertes JSON-Logging nach AWS CloudWatch (Aufbewahrung 14 Tage).
- Tibber Account Linking – der Bearer Token kommt ausschließlich aus dem Alexa-Kontext.

---

## Sicherheitshinweise

- Die Lambda-Funktion darf nur von diesem Alexa Skill aufgerufen werden (Alexa-Trigger mit Skill-ID); zusätzlich prüft das ASK SDK die Skill-ID jedes Requests.
- Der Tibber Access Token wird **nie** geloggt und nur zur Laufzeit im Speicher gehalten.
- Logs enthalten keine personenbezogenen Daten im Klartext – nur die pseudonyme Alexa User-ID.
- Tibber-Aufrufe haben harte Timeouts (2 s Verbindungsaufbau, 4 s Antwort), damit Alexa nie in ihr 8-Sekunden-Limit läuft.

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
