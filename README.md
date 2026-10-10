# Alexa Tibber Skill

Ein **Amazon Alexa Skill** für dynamische Strompreise via [Tibber](https://tibber.com/de) – läuft als **AWS-Lambda-Funktion** mit **Java 25** und dem **ASK SDK**.

Frag Alexa einfach nach dem aktuellen Strompreis, deinem Verbrauch oder der günstigsten bzw. teuersten Stunde des Tages – direkt aus deinem Tibber-Konto.

---

## Für Nutzerinnen und Nutzer

### Was der Skill kann

Mit einem dynamischen Stromtarif von Tibber ändert sich der Preis jede Viertelstunde. Der Skill sagt dir per Sprache, was Strom gerade kostet und wann es sich lohnt, die Waschmaschine, den Geschirrspüler oder das Laden des E-Autos zu starten.

„…“ steht für „Alexa, frag tibber strompreis,“.

| Du sagst (Beispiel) | Alexa antwortet mit |
|---|---|
| „… was kostet Strom gerade?“ | dem Preis der laufenden Viertelstunde in Cent pro Kilowattstunde und dem Preisniveau (z. B. „günstig“ oder „teuer“) |
| „… was kostet Strom morgen um 18 Uhr?“ | dem Preis zu dieser Uhrzeit (heute oder morgen) |
| „… wie teuer wird Strom morgen?“ | Spanne, Durchschnitt sowie günstigster und teuerster Zeitpunkt des Tages |
| „… wann ist Strom am günstigsten?“ / „… am teuersten?“ | dem Beginn der günstigsten bzw. teuersten Stunde mit Durchschnittspreis |
| „… wann sind die günstigsten drei Stunden?“ / „… wann soll ich die Waschmaschine anmachen?“ | dem günstigsten zusammenhängenden Zeitraum – fehlt die Dauer, fragt Alexa nach |
| „… ist Strom gerade günstig?“ | Ja oder Nein nach Preisniveau, sonst mit Hinweis auf die günstigste Stunde |
| „… wie setzt sich der Strompreis zusammen?“ | Energiepreis sowie Steuern und Abgaben |
| „… wie viel Strom habe ich gestern verbraucht?“ / „… was hat mich Strom im September gekostet?“ | Verbrauch, Kosten und Durchschnittspreis für einen Tag, eine Woche, einen Monat oder ein Jahr |
| „… wie viel habe ich letzte Woche eingespeist?“ | Einspeisung und Erlös (nur mit Einspeisung, z. B. PV-Anlage) |
| „… wie viel Watt verbrauche ich gerade?“ | mit Tibber Pulse: aktuelle Leistung und Verbrauch seit Mitternacht; sonst die zuletzt abgerechnete Stunde |

Du kannst den Skill auch mit „Alexa, öffne tibber strompreis“ starten und dann deine Frage stellen. „Hilfe“ nennt dir alle Möglichkeiten, „Stopp“ beendet den Skill. Alle Uhrzeiten gelten in deutscher Zeit.

### Was du brauchst

- ein Alexa-fähiges Gerät oder die Alexa-App auf dem Smartphone,
- ein Tibber-Konto mit aktivem Stromvertrag,
- für Verbrauchs- und Einspeiseangaben ein von Tibber ausgelesenes Smart Meter oder einen [Tibber Pulse](https://tibber.com/de/pulse); Live-Werte in Watt gibt es nur mit Pulse,
- einen persönlichen Tibber-Zugangstoken (Access Token) – wie du ihn erzeugst, steht unten.

### Einrichtung in vier Schritten

1. **Token erzeugen:** Melde dich auf [developer.tibber.com](https://developer.tibber.com/) mit deinem Tibber-Konto an, öffne *Access Token* und erzeuge einen Token. Kopiere ihn.
2. **Skill aktivieren:** Suche in der Alexa-App unter *Mehr* → *Skills & Spiele* nach dem Skill und tippe auf *Zur Verwendung aktivieren*.
3. **Konto verknüpfen:** Die Alexa-App öffnet die Verknüpfungsseite. Füge dort deinen Token ein und tippe auf *Verknüpfen*. Der Token wird bei Tibber geprüft; bei einem Tippfehler zeigt die Seite einen Hinweis.
4. **Fragen:** „Alexa, frag tibber strompreis, was kostet Strom gerade?“

### Deine Daten

- Der Tibber-Token lässt sich nicht auf Lesezugriff beschränken – er gewährt vollen Zugriff auf die Tibber-API deines Kontos. Der Skill nutzt ihn ausschließlich lesend für Preise, Verbrauch, Einspeisung und Live-Messwerte.
- Nach der Verknüpfung speichert Amazon den Token und schickt ihn bei jeder Frage an den Skill mit. Der Skill selbst speichert ihn nicht und schreibt ihn nicht in Logs.
- Du kannst jederzeit aussteigen: Verknüpfung in der Alexa-App beim Skill aufheben und den Token auf developer.tibber.com löschen.
- Details stehen in der Datenschutzerklärung, die in der Skill-Beschreibung im Alexa Skill Store verlinkt ist.

### Häufige Fragen

| Frage | Antwort |
|---|---|
| Alexa sagt „Deine Verknüpfung mit Tibber ist nicht mehr gültig“. | Der Token wurde bei Tibber gelöscht oder ist ungültig. Erzeuge einen neuen Token und verknüpfe das Konto in der Alexa-App neu. |
| Alexa sagt „Bitte verknüpfe zunächst deinen Tibber Account“. | Die Verknüpfung fehlt noch – siehe Schritt 3 der Einrichtung. |
| Es kommen keine Verbrauchsdaten. | Verbrauchsdaten gibt es nur mit Tibber Pulse oder einem von Tibber ausgelesenen Smart Meter. Die letzte Stunde wird außerdem erst mit etwas Verzögerung abgerechnet. |
| Die günstigste Stunde liegt nie am nächsten Tag. | Tibber veröffentlicht die Preise für morgen in der Regel am frühen Nachmittag. Vorher kennt der Skill nur die Preise für heute. |
| Alexa nennt statt der aktuellen Leistung die letzte Stunde. | Live-Werte gibt es nur mit Tibber Pulse. Kommt innerhalb weniger Sekunden keine Messung, antwortet der Skill mit der zuletzt abgerechneten Stunde. |
| Alexa sagt „Diesen Zeitraum kann ich nicht auswerten“. | Unterstützt werden einzelne Tage, Kalenderwochen, Monate und Jahre (Tage und Wochen bis gut ein Jahr, Monate bis zehn Jahre, Jahre bis 20 Jahre zurück). Wochenenden oder Jahreszeiten kennt der Skill nicht. |
| Alexa meldet ein Problem beim Abrufen. | Tibber war kurz nicht erreichbar. Versuch es nach einem Moment erneut. |

---

## Für Entwickler und Betreiber

Die folgenden Abschnitte beschreiben, wie der Skill gebaut, getestet und auf AWS betrieben wird.

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

Git Bash:
```bash
TIBBER_DEMO_TOKEN=<demo-token> ./gradlew test --tests "*.TibberApiIntegrationTest"
```

PowerShell:
```powershell
$env:TIBBER_DEMO_TOKEN = '<demo-token>'; .\gradlew.bat test --tests "*.TibberApiIntegrationTest"
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
5. *Build* → *Tools* → *Account Linking* öffnen und die angezeigten **Alexa Redirect URLs** kopieren (je Region eine, Form `https://layla.amazon.com/api/skill/link/<ID>`). Noch nicht speichern – die Authorization URI entsteht erst beim Deployment.
6. Eine **Client-ID** festlegen (frei wählbar, 8–128 Zeichen aus Buchstaben, Ziffern, `.`, `_`, `-`, z. B. `tibber-strompreis-skill`).

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
| Parameter LinkingClientId | Client-ID aus A7, Schritt 6 |
| Parameter LinkingRedirectUris | alle Alexa Redirect URLs aus A7, Schritt 5, kommagetrennt ohne Leerzeichen |
| Parameter PrivacyPolicyUrl | https-URL deiner Datenschutzerklärung (für einen öffentlichen Skill Pflicht, siehe „Veröffentlichung“); leer lassen nur für private Tests |
| Confirm changes before deploy | `y` – SAM zeigt vor jeder Änderung an, was passiert |
| Allow SAM CLI IAM role creation | `y` – die Lambda-Funktion braucht eine Ausführungsrolle |
| Disable rollback | `n` – bei Fehlern wird automatisch zurückgerollt |
| Save arguments to configuration file | `y` – speichert die Antworten in `samconfig.toml` (lokal, nicht eingecheckt) |
| SAM configuration file / environment | Enter (Standard) |

Am Ende zeigt SAM unter **Outputs** zwei Werte:
- `SkillEndpointArn`, z. B. `arn:aws:lambda:eu-west-1:123456789012:function:alexa-tibber-skill-TibberSkillFunction-AbC123:live`
- `AccountLinkingAuthorizationUri`, z. B. `https://abc123def4.execute-api.eu-west-1.amazonaws.com/link`

**Endpoint im Skill eintragen:**
1. Alexa Developer Console → dein Skill → *Build* → *Endpoint*.
2. **AWS Lambda ARN** wählen und den ARN unter *Default Region* einfügen. Er muss auf `:live` enden.
3. *Save Endpoints*.

**Account Linking einrichten** (Nutzer hinterlegen damit ihren eigenen Tibber-Token):
1. Alexa Developer Console → dein Skill → *Build* → *Tools* → *Account Linking*.
2. *Do you allow users to create an account or link to an existing account with you?* aktivieren.
3. *Allow users to enable skill without account linking* **deaktivieren** – ohne Token kann der Skill nichts abfragen.
4. *Auth Grant Type*: **Implicit Grant**.
5. *Your Web Authorization URI*: der Output `AccountLinkingAuthorizationUri`.
6. *Your Client ID*: die Client-ID aus A7, Schritt 6 (muss exakt mit `LinkingClientId` übereinstimmen).
7. *Scope*: wird nicht ausgewertet; falls die Konsole einen Wert verlangt, `prices` eintragen.
8. *Domain List*: leer lassen (die Seite lädt keine externen Ressourcen).
9. *Save*.

So läuft die Verknüpfung für Nutzer ab: In der Alexa-App den Skill aktivieren → *Konto verknüpfen* → die Verknüpfungsseite öffnet sich → persönlichen Token von [developer.tibber.com](https://developer.tibber.com/) (*Access Token*) einfügen → *Verknüpfen*. Die Seite prüft den Token bei Tibber und gibt ihn an Alexa weiter. Gespeichert wird er nur bei Amazon, nicht von diesem Dienst. Widerruft ein Nutzer den Token später bei Tibber, fordert der Skill beim nächsten Aufruf zur Neuverknüpfung auf.

**Testen:** Reiter *Test* → *Skill testing is enabled in: Development* → „frag tibber strompreis was kostet strom gerade“.

### Teil C – Updates ausrollen

```powershell
aws sso login --profile alexa-tibber    # nur falls die Anmeldung abgelaufen ist
.\gradlew.bat build
sam deploy --profile alexa-tibber       # nutzt die gespeicherten Antworten aus samconfig.toml
```

Stacks, die vor der Einführung des Account Linkings deployt wurden, brauchen einmalig ein `sam deploy --guided --profile alexa-tibber`, um die neuen Parameter `LinkingClientId`, `LinkingRedirectUris` und `PrivacyPolicyUrl` einzugeben; ein einfaches `sam deploy` mit der alten `samconfig.toml` schlägt fehl, weil die Pflichtparameter keinen Standardwert haben.

Jedes Deployment veröffentlicht eine neue Lambda-Version; SnapStart erstellt dafür einen Snapshot, und der Alias `live` zeigt automatisch darauf. In der Alexa Developer Console muss nichts geändert werden. Ausnahme: Änderungen an `skill-package/interactionModels/custom/de-DE.json` müssen zusätzlich im JSON-Editor der Alexa Developer Console eingefügt und dort neu gebaut werden (`sam deploy` aktualisiert das Interaktionsmodell nicht).

### Logs und Fehlersuche

Logs live mitlesen:
```powershell
sam logs --cw-log-group /aws/lambda/alexa-tibber-skill --tail --profile alexa-tibber
```
Logs der Verknüpfungsseite: `sam logs --cw-log-group /aws/lambda/alexa-tibber-skill-account-linking --tail --profile alexa-tibber`

Der Name der Log-Gruppe folgt dem Stack-Namen (`/aws/lambda/<Stack-Name>`); wurde der Stack unter einem anderen Namen angelegt, den Namen entsprechend anpassen.
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
| Verknüpfungsseite zeigt „Unzulässige Weiterleitungsadresse“ | `LinkingRedirectUris` enthält nicht exakt die Alexa Redirect URLs aus der Developer Console → mit `sam deploy --guided --profile alexa-tibber` korrigieren |
| Verknüpfungsseite zeigt „Unbekannter Client“ | Client-ID in der Developer Console weicht von `LinkingClientId` ab |
| Verknüpfungsseite zeigt „Tibber hat diesen Token nicht akzeptiert“ | Token unvollständig kopiert oder bei Tibber widerrufen → neuen Token auf developer.tibber.com erzeugen |
| Alexa sagt „Deine Verknüpfung mit Tibber ist nicht mehr gültig“ | Token wurde bei Tibber widerrufen → in der Alexa-App neu verknüpfen |

### Veröffentlichung als öffentlicher Skill

Vor dem Einreichen zur Zertifizierung:

- [ ] **Datenschutzerklärung** unter einer https-URL veröffentlichen und sowohl als Parameter `PrivacyPolicyUrl` als auch unter *Distribution* → *Privacy Policy URL* eintragen. Sie muss mindestens beschreiben:
  - welche Daten verarbeitet werden: persönlicher Tibber-Token, Preis- und Verbrauchsdaten von Tibber, pseudonymisierte Alexa User-ID in Logs;
  - dass der Tibber-Token nicht auf Lesezugriff beschränkbar ist und vollen Zugriff auf die Tibber-API des Kontos gewährt (der Skill nutzt ihn ausschließlich lesend);
  - dass die Lambda-Funktion des Betreibers den Token mit jeder Anfrage erhält und Nutzer dem Betreiber daher vertrauen müssen;
  - Tibber als Empfänger der Daten bei der Token-Prüfung und bei jeder Abfrage;
  - AWS als Auftragsverarbeiter (Hosting in `eu-west-1`, Auftragsverarbeitungsvertrag);
  - dass Amazon den Token speichert und mit jeder Anfrage an die Lambda-Funktion sendet und dass dieser Dienst ihn nicht speichert;
  - Speicherort und -dauer: AWS-Region `eu-west-1`, Logs 14 Tage;
  - wie Nutzer die Verknüpfung aufheben (Alexa-App) und den Token widerrufen (developer.tibber.com);
  - Betreiber und Kontakt.

  Den Text juristisch prüfen lassen; das Repository enthält bewusst keinen Rechtstext.
- [ ] Verknüpfung in der Alexa-App mit einem echten Konto testen: verknüpfen, Preis abfragen, Token bei Tibber widerrufen, erneut fragen (Aufforderung zur Neuverknüpfung), neu verknüpfen.
- [ ] Prüfen, dass die Verknüpfungsseite auf dem Smartphone gut lesbar ist und Fehlermeldungen anzeigt (z. B. bei einem falsch kopierten Token).
- [ ] In der Skill-Beschreibung erwähnen, dass ein Tibber-Konto und ein persönlicher Access Token nötig sind.

Hinweis: Ist ein Skill mit Account Linking einmal veröffentlicht, lässt sich Account Linking nicht mehr deaktivieren.

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
                                                 │       ├── Price-/Energy-/LiveService → TibberGraphQlClient (GraphQL via java.net.http)
                                                 │       └── LiveMeasurementClient      (GraphQL-Subscription via java.net.http.WebSocket)
                                                 ├── UnhandledRequestHandler  (Catch-all für unbekannte Intents)
                                                 ├── GenericExceptionHandler  (Fallback-Antwort bei Exceptions)
                                                 └── MDCCleanupInterceptor    (MDC leeren)

Alexa-App ──(HTTPS, gedrosselt)──▶ API Gateway /link ──▶ AccountLinkingHandler
                                                 ├── GET:  Formular (OAuth-Parameter geprüft)
                                                 └── POST: Token-Format + Prüfung bei Tibber ──▶ 302 an Amazon-Redirect-URL
```

- Kein Web-Server und kein DI-Framework – der Skill wird beim Lambda-Init einmal gebaut und per SnapStart gesichert.
- Strukturiertes JSON-Logging nach AWS CloudWatch (Aufbewahrung 14 Tage).
- Account Linking per OAuth 2.0 Implicit Grant: Nutzer hinterlegen ihren persönlichen Tibber-Token auf der Verknüpfungsseite; Amazon speichert ihn und schickt ihn mit jeder Anfrage mit (`context.System.user.accessToken`).

---

## Sicherheitshinweise

- Die Lambda-Funktion darf nur von diesem Alexa Skill aufgerufen werden (Alexa-Trigger mit Skill-ID); zusätzlich prüft das ASK SDK die Skill-ID jedes Requests.
- Der Tibber Access Token wird **nie** geloggt und nur zur Laufzeit im Speicher gehalten.
- Logs enthalten als einzigen Personenbezug die pseudonymisierte Alexa User-ID (nach DSGVO ein personenbezogenes Datum); sie wird 14 Tage aufbewahrt und danach gelöscht.
- Tibber-Aufrufe haben harte Timeouts (2 s Verbindungsaufbau, 4 s Antwort), damit Alexa nie in ihr 8-Sekunden-Limit läuft.
- Die Verknüpfungsseite leitet ausschließlich an die exakt konfigurierten Amazon-Redirect-URLs weiter; die Liste wird beim Start auf https, Amazon-Host und Alexa-Link-Pfad geprüft.
- Die Verknüpfungsseite speichert keine Tokens, loggt sie nicht und gibt sie nie im HTML aus. Sie läuft ohne JavaScript und mit strenger Content-Security-Policy, Schutz gegen Einbettung (Clickjacking), `no-store` und `Referrer-Policy: same-origin` (kein Referrer an fremde Seiten; nötig, damit Browser beim Absenden die echte Herkunft mitsenden).
- Alle reflektierten Parameter werden escaped. Steuerzeichen, überlange Werte, doppelte Felder und Formulare fremder Herkunft werden abgelehnt.
- Der Endpunkt ist je Route (GET und POST `/link`) auf 5 Anfragen/s, Burst 10, gedrosselt; API-Gateway-Access-Logs sind bewusst deaktiviert.
- Restrisiko: Die Drosselung gilt global, nicht je Client. Ein einzelner Client kann die Verknüpfungsseite für alle vorübergehend unerreichbar machen (Verfügbarkeit, keine Daten). Ein Überflutungsangriff kann zudem begrenzte Kosten verursachen (API Gateway und Lambda); der Budget-Alarm aus Schritt A5 meldet sie. HTTP APIs unterstützen kein AWS WAF; für dieses Projekt akzeptiert.
- Widerruft ein Nutzer seinen Token bei Tibber, erkennt der Skill das und fordert zur Neuverknüpfung auf.

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

Offene Aufgaben, technische Schulden und Feature Requests stehen in der [BACKLOG.md](BACKLOG.md).

---

## Lizenz

Dieses Projekt steht unter der [MIT License](LICENSE).

---
