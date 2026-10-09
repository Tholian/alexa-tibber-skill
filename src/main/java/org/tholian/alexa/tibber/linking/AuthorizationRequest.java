package org.tholian.alexa.tibber.linking;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * KI-generiert: Geprüfte Parameter einer Account-Linking-Anfrage von Alexa (OAuth 2.0 Implicit Grant).
 *
 * @param state       von Alexa vergebener Wert, wird unverändert zurückgegeben
 * @param clientId    Client-ID aus der Developer Console
 * @param redirectUri exakt erlaubte Amazon-Redirect-URL
 * @param scope       optionaler Scope (wird nicht ausgewertet)
 */
public record AuthorizationRequest( String state, String clientId, String redirectUri, @Nullable String scope ) {

	static final int MAX_PARAMETER_LENGTH = 2048;

	/**
	 * KI-generiert: Prüft die Parameter streng gegen die Konfiguration.
	 * <p>
	 * SICHERHEIT: {@code redirect_uri} wird zuerst und exakt gegen die Allowlist verglichen – sonst könnte
	 * ein Angreifer den Token an eine eigene Adresse umleiten. Bei ungültiger {@code redirect_uri} wird nie
	 * umgeleitet. Steuerzeichen (z. B. CRLF) und überlange Werte werden abgelehnt.
	 *
	 * @throws InvalidAuthorizationRequestException mit einer nutzertauglichen Meldung ohne Parameterwerte
	 */
	public static AuthorizationRequest validate( Map<String, String> params, LinkingSettings settings ) {
		String redirectUri = param( params, "redirect_uri" );
		if ( redirectUri == null || !settings.allowedRedirectUris( ).contains( redirectUri ) ) {
			throw new InvalidAuthorizationRequestException( "Unzulässige Weiterleitungsadresse." );
		}
		if ( !"token".equals( param( params, "response_type" ) ) ) {
			throw new InvalidAuthorizationRequestException( "Nicht unterstützter Anfragetyp." );
		}
		if ( !settings.clientId( ).equals( param( params, "client_id" ) ) ) {
			throw new InvalidAuthorizationRequestException( "Unbekannter Client." );
		}
		String state = param( params, "state" );
		if ( state == null || state.isEmpty( ) ) {
			throw new InvalidAuthorizationRequestException( "Fehlender state-Parameter." );
		}
		return new AuthorizationRequest( state, settings.clientId( ), redirectUri, param( params, "scope" ) );
	}

	/**
	 * KI-generiert: Ziel der Weiterleitung nach erfolgreicher Token-Prüfung. Der Token steht im URL-Fragment
	 * (Implicit Grant) – Fragmente werden vom Browser nicht an Server übertragen, nur an die Amazon-Seite.
	 */
	public String successRedirect( String accessToken ) {
		return redirectUri + "#state=" + encode( state ) + "&access_token=" + encode( accessToken ) + "&token_type=Bearer";
	}

	private static @Nullable String param( Map<String, String> params, String name ) {
		String value = params.get( name );
		if ( value != null && ( value.length( ) > MAX_PARAMETER_LENGTH || value.chars( ).anyMatch( Character::isISOControl ) ) ) {
			throw new InvalidAuthorizationRequestException( "Ungültiger Parameter." );
		}
		return value;
	}

	// KI-generiert: RFC-3986-konform (Leerzeichen als %20 statt '+')
	private static String encode( String value ) {
		return URLEncoder.encode( value, StandardCharsets.UTF_8 ).replace( "+", "%20" );
	}
}
