package org.tholian.alexa.tibber;

import java.util.Map;

import com.amazon.ask.SkillStreamHandler;

import org.tholian.alexa.tibber.config.SkillFactory;
import org.tholian.alexa.tibber.config.SkillSettings;

/**
 * KI-generiert: Einstiegspunkt der AWS-Lambda-Funktion
 * (Handler: {@code org.tholian.alexa.tibber.TibberSkillStreamHandler::handleRequest}).
 * <p>
 * Der Skill wird einmalig pro Execution Environment in der Init-Phase gebaut. Mit SnapStart
 * landet dieser Zustand im Snapshot, sodass Kaltstarts weitgehend entfallen.
 * <p>
 * SICHERHEIT: Aufrufe sind doppelt abgesichert – durch die Resource Policy der Lambda
 * (Alexa-Trigger mit Skill-ID, siehe template.yaml) und durch die Skill-ID-Prüfung im ASK SDK.
 * Eine eigene Request-Signaturprüfung ist nur bei HTTPS-Endpoints nötig, nicht bei Lambda.
 */
public class TibberSkillStreamHandler extends SkillStreamHandler {

	/** KI-generiert: Von AWS Lambda verwendeter Konstruktor – liest die Konfiguration aus den Umgebungsvariablen. */
	public TibberSkillStreamHandler( ) {
		this( System.getenv( ) );
	}

	/** KI-generiert: Testbarer Konstruktor mit expliziter Umgebung (kein Manipulieren von System.getenv nötig). */
	TibberSkillStreamHandler( Map<String, String> environment ) {
		super( SkillFactory.create( SkillSettings.fromEnvironment( environment ) ) );
	}
}
