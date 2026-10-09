package org.tholian.alexa.tibber.config;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * KI-generiert: Laufzeitkonfiguration des Skills aus den Umgebungsvariablen der Lambda-Funktion.
 *
 * @param skillId      Alexa Skill-ID; Requests anderer Skills werden vom ASK SDK abgewiesen (SICHERHEIT)
 * @param tibberApiUrl Endpoint der Tibber GraphQL API
 */
public record SkillSettings( String skillId, URI tibberApiUrl ) {

	public static final String ENV_SKILL_ID = "ALEXA_SKILL_ID";
	public static final String ENV_TIBBER_API_URL = "TIBBER_API_URL";
	// KI-generiert: Hosts, für die unverschlüsseltes http erlaubt ist (nur lokale Tests gegen den Fake-Server)
	private static final Set<String> LOOPBACK_HOSTS = Set.of( "localhost", "127.0.0.1", "[::1]" );
	public static final URI DEFAULT_TIBBER_API_URL = URI.create( "https://api.tibber.com/v1-beta/gql" );

	/**
	 * KI-generiert: Liest die Konfiguration aus einer Umgebungs-Map (in Produktion {@code System.getenv()}).
	 * Fehlt die Skill-ID, schlägt bereits der Lambda-Init fehl – lieber laut scheitern als jeden
	 * Aufrufer zu akzeptieren.
	 * <p>
	 * SICHERHEIT: {@code TIBBER_API_URL} wird nur vom Deployer gesetzt (SAM-Template); in Produktion
	 * muss sie auf https zeigen, da über sie der Bearer-Token übertragen wird.
	 */
	public static SkillSettings fromEnvironment( Map<String, String> env ) {
		String skillId = env.get( ENV_SKILL_ID );
		if ( skillId == null || skillId.isBlank( ) ) {
			throw new IllegalStateException( "Umgebungsvariable " + ENV_SKILL_ID
			                                 + " fehlt – ohne Skill-ID würde jeder Aufrufer akzeptiert." );
		}
		String apiUrl = env.get( ENV_TIBBER_API_URL );
		URI tibberApiUrl = ( apiUrl == null || apiUrl.isBlank( ) ) ? DEFAULT_TIBBER_API_URL : parseApiUrl( apiUrl.strip( ) );
		return new SkillSettings( skillId.strip( ), tibberApiUrl );
	}

	/**
	 * KI-generiert: Validiert die Tibber-URL beim Init (fail fast). Erlaubt sind absolute https-URLs;
	 * http nur für Loopback-Hosts. SICHERHEIT: Über diese URL wird der Bearer-Token gesendet – ein
	 * Klartext-Endpoint oder eine Fehlkonfiguration darf nie unbemerkt bleiben.
	 */
	private static URI parseApiUrl( String value ) {
		URI uri;
		try {
			uri = URI.create( value );
		} catch ( IllegalArgumentException e ) {
			throw new IllegalStateException( "Umgebungsvariable " + ENV_TIBBER_API_URL + " ist keine gültige URI.", e );
		}
		String scheme = uri.getScheme( );
		String host = uri.getHost( );
		boolean https = "https".equalsIgnoreCase( scheme );
		boolean httpLoopback = "http".equalsIgnoreCase( scheme ) && host != null
		                       && LOOPBACK_HOSTS.contains( host.toLowerCase( Locale.ROOT ) );
		if ( !uri.isAbsolute( ) || host == null || !( https || httpLoopback ) ) {
			throw new IllegalStateException( "Umgebungsvariable " + ENV_TIBBER_API_URL
			                                 + " muss eine absolute https-URL sein (http nur für localhost)." );
		}
		return uri;
	}
}
