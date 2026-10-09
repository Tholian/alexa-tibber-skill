# Backlog

Zentrale Liste für offene Aufgaben, technische Schulden und Feature Requests dieses Skills.

**So wird die Liste gepflegt:**
- Neue Einträge im passenden Abschnitt ergänzen, Format: `- [ ] **Kurztitel** – Beschreibung. _Kontext, Datum (JJJJ-MM-TT)_`
- Erledigte Einträge abhaken und mit Datum und Commit nach „Erledigt“ verschieben.
- Keine personenbezogenen Daten, Tokens oder Zugangsdaten eintragen.

---

## Vor der Veröffentlichung (Pflicht)

- [ ] **Datenschutzerklärung erstellen, prüfen und veröffentlichen** – Pflicht für einen öffentlichen Skill mit Account Linking. Die URL wird als SAM-Parameter `PrivacyPolicyUrl` (Link auf der Verknüpfungsseite) und in der Alexa Developer Console unter *Distribution* → *Privacy Policy URL* eingetragen. Mindestinhalte:
  - **Wichtiger Hinweis zum Tibber-Token:** Der persönliche Tibber-Token lässt sich nicht auf Lesezugriff beschränken. Er gewährt vollen Zugriff auf die Tibber-API des Kontos. Der Skill nutzt ihn ausschließlich lesend (Preise, Verbrauch, Home-IDs zur Prüfung). Das muss für Nutzer klar erkennbar offengelegt werden.
  - Welche Daten verarbeitet werden: persönlicher Tibber-Token, Preis- und Verbrauchsdaten von Tibber, pseudonymisierte Alexa User-ID in Logs (nach DSGVO ein personenbezogenes Datum).
  - Betreiberzugriff und Vertrauen: Die Lambda-Funktion des Betreibers erhält den Token mit jeder Anfrage; Nutzer müssen dem Betreiber vertrauen.
  - Empfänger und Auftragsverarbeiter: Tibber erhält den Token bei der Prüfung und bei jeder Abfrage (Empfänger); AWS verarbeitet die Daten im Auftrag (Auftragsverarbeiter).
  - Wo der Token liegt: Amazon speichert ihn nach der Verknüpfung und sendet ihn mit jeder Anfrage an die Lambda-Funktion. Dieser Dienst speichert ihn nicht und loggt ihn nicht.
  - Speicherort und -dauer: AWS-Region `eu-west-1`, Logs 14 Tage.
  - Widerruf: Verknüpfung in der Alexa-App aufheben, Token auf developer.tibber.com widerrufen.
  - Betreiber und Kontakt (Betreiberdaten nur in der veröffentlichten Erklärung, nicht im Repository).

  Den Text juristisch prüfen lassen; das Repository enthält bewusst keinen Rechtstext. _Account-Linking-Plan, 2026-10-09_
- [ ] **Implicit Grant in der Developer Console bestätigen** – prüfen, dass *Implicit Grant* auswählbar ist und ob das Scope-Feld einen Wert verlangt (laut README dann `prices`). _2026-10-09_
- [ ] **Tibber-Ablehnungsverhalten verifizieren** – Integrationstest `verifyToken_withInvalidToken_throwsAuthenticationException` mit gesetztem `TIBBER_DEMO_TOKEN` ausführen. Schlägt er fehl, liefert Tibber einen anderen Fehlercode, und `TibberService.isUnauthenticated` muss angepasst werden. _2026-10-09_
- [ ] **Markenrecht für Skill-Name klären** – Skill-Name („Tibber Strompreis“) und Invocation Name (`tibber strompreis`) enthalten die Marke „Tibber“. Amazon lehnt bei der Zertifizierung Skills ab, die fremde Markennamen ohne Nachweis einer Berechtigung verwenden. Vor dem Einreichen eine schriftliche Erlaubnis von Tibber einholen oder einen neutralen Namen wählen (dann Invocation Name in `skill-package/interactionModels/custom/de-DE.json`, Texte in den Handlern, README und Tests anpassen). _2026-10-09_
- [ ] **Lambda-Runtime prüfen** – sicherstellen, dass `java25` mit SnapStart in `eu-west-1` verfügbar ist; sonst auf `java21` und Toolchain 21 umstellen (siehe README-Fehlertabelle). _2026-10-09_
- [ ] **Ende-zu-Ende-Test auf dem Smartphone** – verknüpfen, Preis abfragen, Token bei Tibber widerrufen, erneut fragen (Aufforderung zur Neuverknüpfung), neu verknüpfen. Fehleranzeige bei falsch kopiertem Token prüfen. Zusätzlich prüfen:
  - Die Weiterleitung zu Amazon wird nicht durch die CSP der Seite blockiert (`form-action`; Browser prüfen jeden Redirect-Schritt).
  - Der Datenschutz-Link öffnet sich in der WebView der Alexa-App; falls nicht, dessen Domain in der Account-Linking-*Domain List* ergänzen.
  - Ein `+` und Sonderzeichen in `state` überstehen die Query-Dekodierung unverändert.

  _2026-10-09_

## Technische Aufgaben

- [ ] **Dependabot-Sicherheitsüberwachung aktivieren** – Im GitHub-Repository unter *Settings → Code security* „Dependabot alerts“ und „Dependabot security updates“ einschalten. `.github/dependabot.yml` steuert nur die regulären Versions-Updates; die Sicherheits-PRs laufen über diese Einstellungen. Wirkt erst, wenn die Konfiguration auf `main` liegt. _2026-10-09_
- [ ] **Logging-Interceptoren im Projektstil** – `LoggingInterceptor` und `MDCCleanupInterceptor` auf Tabs und `foo( x )` umstellen. _Abschluss-Review Lambda-Migration, 2026-10-09_
- [ ] **KI-Kennzeichnung der Handler-Konstruktoren** – `KI-generiert:`-Kommentare an den Konstruktoren der Tibber-Handler ergänzen. _2026-10-09_
- [ ] **Null-Guard im `UnhandledRequestHandler`** – `getIntent()` kann bei fehlerhaften Requests `null` sein. _2026-10-09_
- [ ] **Reprompt-Text zentralisieren** – „Wie kann ich dir helfen?“ steht doppelt in `GenericExceptionHandler` und `UnhandledRequestHandler`. _2026-10-09_
- [ ] **Robustere Antwortprüfung** – unvollständige Tibber-Antworten (z. B. `{"data":{}}`) als `TibberApiException` statt `NullPointerException` melden. _2026-10-09_
- [ ] **`@SuppressWarnings("unchecked")` in `SkillFactory` entfernen** – stattdessen die `List`-Überladungen von `addRequestHandlers` und `addRequest-/ResponseInterceptors` nutzen. _2026-10-09_
- [ ] **Mockito-Agent per `jvmArgumentProviders`** einbinden (kompatibel zum Gradle Configuration Cache). _2026-10-09_
- [ ] **Leisere Testausgabe** – erwartete Fehlerpfade in `logback-test.xml` nicht mit vollem Stacktrace loggen. _2026-10-09_
- [ ] **Git-Historie** – ältere Commits enthalten die öffentlichen Tibber-Demo-Tokens. Vor einer Veröffentlichung des Repositories bewusst entscheiden, ob die Historie bereinigt werden soll. _2026-10-09_
- [ ] **Drosselung pro Client** – Die Verknüpfungsseite ist nur global (je Route 5 Anfragen/s) gedrosselt; ein einzelner Client kann sie vorübergehend für alle blockieren. Bei Bedarf API Gateway REST API mit AWS WAF oder eine vorgeschaltete CloudFront-Distribution mit WAF prüfen. _2026-10-09_
- [ ] **Origin-Prüfung hinter Proxy** – Die Herkunftsprüfung vergleicht mit „https://“ + Host. Wird später ein Proxy vorgeschaltet, der den Host-Header umschreibt (z. B. CloudFront), muss die erwartete Origin konfigurierbar werden. _2026-10-09_

### Nacharbeiten Account Linking

- [ ] **Robustere Antwortprüfung erweitern** – 200 mit JSON-`null` meldet „HTTP 200“ statt „nicht lesbar“; Null-Prüfungen auch in `firstHome`. _Final-Review Account Linking, 2026-10-09_
- [ ] **Blank-Token-Test für alle vier Tibber-Handler** – außerdem vollqualifizierte Namen in Tests (`verifyNoInteractions`, `URLEncoder`, `JsonNode`) durch Imports ersetzen. _Final-Review Account Linking, 2026-10-09_
- [ ] **Compact-Konstruktoren für `LinkingSettings`/`AuthorizationRequest`** – Allowlist kopieren und validieren, damit die Validierung nicht über die Record-Konstruktoren umgangen werden kann. _Final-Review Account Linking, 2026-10-09_
- [ ] **`KI-generiert:`-Präfix im Javadoc von `TibberApiUrls.parse`** ergänzen. _Final-Review Account Linking, 2026-10-09_
- [ ] **Weitere Regressionstests** – Redirect-URLs (Großschreibung, Punkt am Ende, `:443`), `successRedirect` mit Nicht-ASCII/`#`, `+`-Dekodierung und kodierte Duplikate in `FormData`, Größenprüfung vor dem Base64-Dekodieren. _Final-Review Account Linking, 2026-10-09_
- [ ] **`Allow`-Header bei 405** – bei nicht erlaubter Methode den Header `Allow: GET, POST` mitsenden. _Final-Review Account Linking, 2026-10-09_
- [ ] **Logback-Version im Build nur einmal pflegen.** _Final-Review Account Linking, 2026-10-09_
- [ ] **Template-Feinschliff** – `PrivacyPolicyUrl`-Pattern im Template an die Java-Prüfung angleichen; Parameterbeschreibung `LinkingRedirectUris` um „ohne Leerzeichen“ ergänzen. _Final-Review Account Linking, 2026-10-09_
- [ ] **README-Feinschliff** – „Pflichtparameter“ präzisieren (nur `LinkingClientId`/`LinkingRedirectUris` ohne Standardwert), `PrivacyPolicyUrl`-Zeile vs. „Optional“, Satz zur Log-Gruppe umstellen, Fehlertabelle um „ungültiges Format“, „Prüfung bei Tibber fehlgeschlagen“ und Init-Fehler der Linking-Lambda ergänzen; CLAUDE.md-Logging um die Gruppe `-account-linking`. _Final-Review Account Linking, 2026-10-09_
- [ ] **Passwortmanager** – Browser bieten an, den Vollzugriffs-Token zu speichern (`type=password`); bewusst entscheiden (z. B. `autocomplete`-Variante testen). _Final-Review Account Linking, 2026-10-09_

## Feature Requests

- [ ] **Viertelstunden-Preise** – Tibber liefert zunehmend Preise in 15-Minuten-Auflösung; Abfrage und Sprachausgabe darauf erweitern. _2026-10-09_
- [ ] **Mehrere Zuhause** – aktuell wird immer das erste Zuhause des Kontos verwendet; Auswahl per Sprachbefehl oder Einstellung. _2026-10-09_
- [ ] **Günstigstes Zeitfenster für Geräte** – z. B. „Wann ist der günstigste 2-Stunden-Block für die Waschmaschine?“. _2026-10-09_
- [ ] **Preise für morgen gezielt abfragen** – z. B. „Was kostet Strom morgen um 18 Uhr?“. _2026-10-09_

## Erledigt

- [x] **Umstellung auf AWS Lambda** – Skill läuft als Lambda-Funktion mit SnapStart, Deployment per SAM. _2026-10-09, Commits 0225232..1100346 (Git-Bereich, ohne 0225232 selbst)_
- [x] **Account Linking mit persönlichem Tibber-Token** – gehärtete Verknüpfungsseite (Implicit Grant), Neuverknüpfung bei widerrufenem Token. _2026-10-09, Commits 1100346..3268431 sowie die Backlog- und Fix-Commits (bis einschließlich der abschließenden Fix-Commits)_
