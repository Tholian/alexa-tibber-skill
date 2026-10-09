package org.tholian.alexa.tibber.config;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * KI-generiert: Gemeinsame Validierung der Tibber-API-URL für Skill- und Account-Linking-Funktion.
 * <p>
 * SICHERHEIT: Über diese URL wird der Bearer-Token gesendet – erlaubt sind nur absolute https-URLs,
 * http ausschließlich für Loopback-Hosts (lokale Tests gegen den Fake-Server).
 */
public final class TibberApiUrls {

	public static final URI DEFAULT = URI.create( "https://api.tibber.com/v1-beta/gql" );
	private static final Set<String> LOOPBACK_HOSTS = Set.of( "localhost", "127.0.0.1", "[::1]" );

	private TibberApiUrls( ) {
	}

	/**
	 * @param value        bereits getrimmter Wert der Umgebungsvariable
	 * @param variableName Name der Umgebungsvariable (für die Fehlermeldung)
	 * @throws IllegalStateException bei ungültiger oder unsicherer URL (fail fast beim Lambda-Init)
	 */
	public static URI parse( String value, String variableName ) {
		URI uri;
		try {
			uri = URI.create( value );
		} catch ( IllegalArgumentException e ) {
			throw new IllegalStateException( "Umgebungsvariable " + variableName + " ist keine gültige URI.", e );
		}
		String scheme = uri.getScheme( );
		String host = uri.getHost( );
		boolean https = "https".equalsIgnoreCase( scheme );
		boolean httpLoopback = "http".equalsIgnoreCase( scheme ) && host != null
		                       && LOOPBACK_HOSTS.contains( host.toLowerCase( Locale.ROOT ) );
		if ( !uri.isAbsolute( ) || host == null || !( https || httpLoopback ) ) {
			throw new IllegalStateException( "Umgebungsvariable " + variableName
			                                 + " muss eine absolute https-URL sein (http nur für localhost)." );
		}
		return uri;
	}
}
