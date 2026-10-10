package org.tholian.alexa.tibber.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

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

	private static final String TIBBER_DOMAIN = "tibber.com";

	/**
	 * KI-generiert: Prüft die von Tibber gemeldete WebSocket-URL, bevor der Token dorthin gesendet wird.
	 * <p>
	 * SICHERHEIT: Die URL stammt aus einer API-Antwort und ist damit nicht vertrauenswürdig konfiguriert.
	 * Erlaubt sind nur {@code wss://} auf {@code tibber.com} bzw. {@code *.tibber.com} und – für Tests –
	 * {@code ws://} auf Loopback. Userinfo-Anteile werden abgelehnt.
	 *
	 * @return die URL, wenn sie zulässig ist, sonst leer
	 */
	public static Optional<URI> webSocketUrl( @Nullable String value ) {
		if ( value == null || value.isBlank( ) ) {
			return Optional.empty( );
		}
		URI uri;
		try {
			uri = new URI( value.strip( ) );
		} catch ( URISyntaxException e ) {
			return Optional.empty( );
		}
		String scheme = uri.getScheme( );
		String host = uri.getHost( );
		if ( !uri.isAbsolute( ) || host == null || uri.getRawUserInfo( ) != null ) {
			return Optional.empty( );
		}
		String lowerHost = host.toLowerCase( Locale.ROOT );
		boolean tibber = "wss".equalsIgnoreCase( scheme )
		                 && ( lowerHost.equals( TIBBER_DOMAIN ) || lowerHost.endsWith( "." + TIBBER_DOMAIN ) );
		boolean loopback = "ws".equalsIgnoreCase( scheme ) && LOOPBACK_HOSTS.contains( lowerHost );
		return tibber || loopback ? Optional.of( uri ) : Optional.empty( );
	}
}
