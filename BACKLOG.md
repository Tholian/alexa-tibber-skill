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
  - Wo der Token liegt: Amazon speichert ihn nach der Verknüpfung und sendet ihn mit jeder Anfrage an die Lambda-Funktion. Dieser Dienst speichert ihn nicht und loggt ihn nicht.
  - Speicherort und -dauer: AWS-Region `eu-west-1`, Logs 14 Tage.
  - Widerruf: Verknüpfung in der Alexa-App aufheben, Token auf developer.tibber.com widerrufen.
  - Betreiber und Kontakt (Betreiberdaten nur in der veröffentlichten Erklärung, nicht im Repository).

  Den Text juristisch prüfen lassen; das Repository enthält bewusst keinen Rechtstext. _Account-Linking-Plan, 2026-10-09_
- [ ] **Implicit Grant in der Developer Console bestätigen** – prüfen, dass *Implicit Grant* auswählbar ist und ob das Scope-Feld einen Wert verlangt (laut README dann `prices`). _2026-10-09_
- [ ] **Tibber-Ablehnungsverhalten verifizieren** – Integrationstest `verifyToken_withInvalidToken_throwsAuthenticationException` mit gesetztem `TIBBER_DEMO_TOKEN` ausführen. Schlägt er fehl, liefert Tibber einen anderen Fehlercode, und `TibberService.isUnauthenticated` muss angepasst werden. _2026-10-09_
- [ ] **Lambda-Runtime prüfen** – sicherstellen, dass `java25` mit SnapStart in `eu-west-1` verfügbar ist; sonst auf `java21` und Toolchain 21 umstellen (siehe README-Fehlertabelle). _2026-10-09_
- [ ] **Ende-zu-Ende-Test auf dem Smartphone** – verknüpfen, Preis abfragen, Token bei Tibber widerrufen, erneut fragen (Aufforderung zur Neuverknüpfung), neu verknüpfen. Fehleranzeige bei falsch kopiertem Token prüfen. _2026-10-09_

## Technische Aufgaben

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

## Feature Requests

- [ ] **Viertelstunden-Preise** – Tibber liefert zunehmend Preise in 15-Minuten-Auflösung; Abfrage und Sprachausgabe darauf erweitern. _2026-10-09_
- [ ] **Mehrere Zuhause** – aktuell wird immer das erste Zuhause des Kontos verwendet; Auswahl per Sprachbefehl oder Einstellung. _2026-10-09_
- [ ] **Günstigstes Zeitfenster für Geräte** – z. B. „Wann ist der günstigste 2-Stunden-Block für die Waschmaschine?“. _2026-10-09_
- [ ] **Preise für morgen gezielt abfragen** – z. B. „Was kostet Strom morgen um 18 Uhr?“. _2026-10-09_

## Erledigt

- [x] **Umstellung auf AWS Lambda** – Skill läuft als Lambda-Funktion mit SnapStart, Deployment per SAM. _2026-10-09, Commits 2059c50..1100346_
- [x] **Account Linking mit persönlichem Tibber-Token** – gehärtete Verknüpfungsseite (Implicit Grant), Neuverknüpfung bei widerrufenem Token. _2026-10-09, Commits 3b78797..3268431 (Tasks 1–6) sowie dieser Backlog-Commit_
