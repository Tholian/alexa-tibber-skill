package org.tholian.alexa.tibber.config;

import java.net.URI;
import java.util.Map;

/**
 * KI-generiert: Laufzeitkonfiguration des Skills aus den Umgebungsvariablen der Lambda-Funktion.
 *
 * @param skillId      Alexa Skill-ID; Requests anderer Skills werden vom ASK SDK abgewiesen (SICHERHEIT)
 * @param tibberApiUrl Endpoint der Tibber GraphQL API
 */
public record SkillSettings( String skillId, URI tibberApiUrl ) {

	public static final String ENV_SKILL_ID = "ALEXA_SKILL_ID";
	public static final String ENV_TIBBER_API_URL = "TIBBER_API_URL";
	public static final URI DEFAULT_TIBBER_API_URL = TibberApiUrls.DEFAULT;

	/**
	 * KI-generiert: Liest die Konfiguration aus einer Umgebungs-Map (in Produktion {@code System.getenv()}).
	 * Fehlt die Skill-ID, schlägt bereits der Lambda-Init fehl – lieber laut scheitern als jeden
	 * Aufrufer zu akzeptieren.
	 * <p>
	 * SICHERHEIT: {@code TIBBER_API_URL} wird nur vom Deployer gesetzt (SAM-Template); in Produktion
	 * muss sie auf https zeigen, da über sie der Bearer-Token übertragen wird (Prüfung in {@link TibberApiUrls}).
	 */
	public static SkillSettings fromEnvironment( Map<String, String> env ) {
		String skillId = env.get( ENV_SKILL_ID );
		if ( skillId == null || skillId.isBlank( ) ) {
			throw new IllegalStateException( "Umgebungsvariable " + ENV_SKILL_ID
			                                 + " fehlt – ohne Skill-ID würde jeder Aufrufer akzeptiert." );
		}
		String apiUrl = env.get( ENV_TIBBER_API_URL );
		URI tibberApiUrl = ( apiUrl == null || apiUrl.isBlank( ) )
		                   ? DEFAULT_TIBBER_API_URL
		                   : TibberApiUrls.parse( apiUrl.strip( ), ENV_TIBBER_API_URL );
		return new SkillSettings( skillId.strip( ), tibberApiUrl );
	}
}
