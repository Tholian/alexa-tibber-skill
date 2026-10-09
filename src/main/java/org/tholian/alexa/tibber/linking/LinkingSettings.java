package org.tholian.alexa.tibber.linking;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.tholian.alexa.tibber.config.SkillSettings;
import org.tholian.alexa.tibber.config.TibberApiUrls;

/**
 * KI-generiert: Konfiguration der Account-Linking-Funktion aus Umgebungsvariablen (fail fast beim Init).
 *
 * @param clientId            Client-ID aus der Alexa Developer Console (Account Linking)
 * @param allowedRedirectUris exakte „Alexa Redirect URLs“ aus der Developer Console
 * @param tibberApiUrl        Tibber GraphQL Endpoint für die Token-Prüfung
 * @param privacyPolicyUrl    optionale Datenschutzerklärung, wird auf der Seite verlinkt
 */
public record LinkingSettings(
		String clientId,
		Set<String> allowedRedirectUris,
		URI tibberApiUrl,
		@Nullable URI privacyPolicyUrl
) {

	public static final String ENV_CLIENT_ID = "LINKING_CLIENT_ID";
	public static final String ENV_REDIRECT_URIS = "LINKING_REDIRECT_URIS";
	public static final String ENV_PRIVACY_POLICY_URL = "PRIVACY_POLICY_URL";

	// KI-generiert: Alexa leitet nur über diese Amazon-Hosts zurück (je Region einer)
	static final Set<String> AMAZON_REDIRECT_HOSTS = Set.of( "layla.amazon.com", "pitangui.amazon.com", "alexa.amazon.co.jp" );
	private static final String REDIRECT_PATH_PREFIX = "/api/skill/link/";
	private static final Pattern CLIENT_ID_FORMAT = Pattern.compile( "[A-Za-z0-9._-]{8,128}" );

	/**
	 * KI-generiert: Liest und prüft die Konfiguration.
	 * SICHERHEIT: Jede Redirect-URL muss https, einen Amazon-Redirect-Host ohne Port/Userinfo und den
	 * Alexa-Link-Pfad haben – so kann selbst eine Fehlkonfiguration keinen Token an fremde Server leiten.
	 */
	public static LinkingSettings fromEnvironment( Map<String, String> env ) {
		String clientId = required( env, ENV_CLIENT_ID );
		if ( !CLIENT_ID_FORMAT.matcher( clientId ).matches( ) ) {
			throw new IllegalStateException( ENV_CLIENT_ID + " muss 8–128 Zeichen aus A–Z, a–z, 0–9, '.', '_' oder '-' enthalten." );
		}

		Set<String> redirectUris = new LinkedHashSet<>( );
		for ( String part : required( env, ENV_REDIRECT_URIS ).split( "," ) ) {
			String uri = part.strip( );
			if ( !uri.isEmpty( ) ) {
				redirectUris.add( validateRedirectUri( uri ) );
			}
		}
		if ( redirectUris.isEmpty( ) ) {
			throw new IllegalStateException( ENV_REDIRECT_URIS + " enthält keine Redirect-URL." );
		}

		String apiUrl = env.get( SkillSettings.ENV_TIBBER_API_URL );
		URI tibberApiUrl = ( apiUrl == null || apiUrl.isBlank( ) )
		                   ? TibberApiUrls.DEFAULT
		                   : TibberApiUrls.parse( apiUrl.strip( ), SkillSettings.ENV_TIBBER_API_URL );

		String privacy = env.get( ENV_PRIVACY_POLICY_URL );
		URI privacyPolicyUrl = ( privacy == null || privacy.isBlank( ) ) ? null : validateHttps( privacy.strip( ) );

		return new LinkingSettings( clientId, Set.copyOf( redirectUris ), tibberApiUrl, privacyPolicyUrl );
	}

	private static String required( Map<String, String> env, String name ) {
		String value = env.get( name );
		if ( value == null || value.isBlank( ) ) {
			throw new IllegalStateException( "Umgebungsvariable " + name + " fehlt." );
		}
		return value.strip( );
	}

	private static String validateRedirectUri( String value ) {
		URI uri;
		try {
			uri = URI.create( value );
		} catch ( IllegalArgumentException e ) {
			throw new IllegalStateException( ENV_REDIRECT_URIS + " enthält eine ungültige URL.", e );
		}
		boolean valid = "https".equals( uri.getScheme( ) )
		                && uri.getHost( ) != null && AMAZON_REDIRECT_HOSTS.contains( uri.getHost( ) )
		                && uri.getPort( ) == -1
		                && uri.getRawUserInfo( ) == null
		                && uri.getRawPath( ) != null && uri.getRawPath( ).startsWith( REDIRECT_PATH_PREFIX )
		                && uri.getRawQuery( ) == null
		                && uri.getRawFragment( ) == null;
		if ( !valid ) {
			throw new IllegalStateException( ENV_REDIRECT_URIS + " enthält eine unzulässige Redirect-URL: " + value );
		}
		return value;
	}

	private static URI validateHttps( String value ) {
		try {
			URI uri = URI.create( value );
			if ( "https".equals( uri.getScheme( ) ) && uri.getHost( ) != null ) {
				return uri;
			}
		} catch ( IllegalArgumentException e ) {
			// KI-generiert: einheitliche Fehlermeldung unten
		}
		throw new IllegalStateException( ENV_PRIVACY_POLICY_URL + " muss eine absolute https-URL sein." );
	}
}
