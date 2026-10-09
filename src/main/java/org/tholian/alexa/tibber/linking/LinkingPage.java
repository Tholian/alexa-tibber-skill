package org.tholian.alexa.tibber.linking;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

/**
 * KI-generiert: Erzeugt die HTML-Seiten des Account Linkings – ohne JavaScript, ohne externe Ressourcen,
 * mobilfreundlich (Anforderung von Amazon: Anzeige in der Alexa-App, keine Pop-ups).
 * <p>
 * SICHERHEIT: Alle reflektierten Werte werden HTML-escaped; die CSP erlaubt nur das eine Inline-Stylesheet
 * (per Hash), kein Skript, kein Framing, Formular-Ziele nur die eigene Seite und Amazon.
 * Der Tibber-Token wird nie in HTML ausgegeben.
 */
final class LinkingPage {

	static final String CSS = "body{font-family:sans-serif;max-width:32rem;margin:1rem auto;padding:0 1rem;line-height:1.5}"
	                          + "label,input,button{display:block;width:100%;box-sizing:border-box}"
	                          + "input{padding:.6rem;margin:.4rem 0 1rem;font-size:1rem}"
	                          + "button{padding:.8rem;font-size:1rem}"
	                          + ".error{color:#b00020;font-weight:bold}";

	static final String CSP = "default-src 'none'; style-src 'sha256-" + sha256Base64( CSS ) + "'; form-action 'self' "
	                          + LinkingSettings.AMAZON_REDIRECT_HOSTS.stream( ).sorted( )
			                          .map( host -> "https://" + host ).collect( Collectors.joining( " " ) )
	                          + "; frame-ancestors 'none'; base-uri 'none'";

	private LinkingPage( ) {
	}

	/** KI-generiert: Formularseite; {@code errorMessage} ist ein fester, nicht nutzergesteuerter Text. */
	static String form( AuthorizationRequest request, @Nullable String errorMessage, @Nullable URI privacyPolicyUrl ) {
		StringBuilder html = new StringBuilder( head( "Tibber-Konto verknüpfen" ) );
		html.append( "<h1>Tibber-Konto verknüpfen</h1>" )
				.append( "<p>Der Skill liest mit deinem persönlichen Tibber-Zugangstoken deine Strompreise und deinen Verbrauch. " )
				.append( "Den Token erzeugst du auf developer.tibber.com unter „Access Token“. " )
				.append( "Öffne die Seite dafür am besten in einem separaten Browser und kopiere den Token hierher.</p>" );
		// SICHERHEIT/Transparenz: Hinweis auf den Vollzugriff direkt an der Einwilligungsstelle (statischer Text)
		html.append( "<p><strong>Hinweis:</strong> Der Tibber-Token lässt sich nicht auf Lesezugriff beschränken und gewährt "
				+ "vollen Zugriff auf die Tibber-API deines Kontos. Alexa schickt ihn bei jeder Frage an diesen Skill, "
				+ "der ihn ausschließlich lesend nutzt.</p>" );
		if ( errorMessage != null ) {
			html.append( "<p class=\"error\" role=\"alert\">" ).append( escape( errorMessage ) ).append( "</p>" );
		}
		html.append( "<form method=\"post\" action=\"/link\">" )
				.append( hidden( "state", request.state( ) ) )
				.append( hidden( "client_id", request.clientId( ) ) )
				.append( hidden( "redirect_uri", request.redirectUri( ) ) )
				.append( hidden( "response_type", "token" ) );
		if ( request.scope( ) != null ) {
			html.append( hidden( "scope", request.scope( ) ) );
		}
		html.append( "<label for=\"tibber_token\">Tibber Access Token</label>" )
				.append( "<input id=\"tibber_token\" name=\"tibber_token\" type=\"password\" autocomplete=\"off\" " )
				.append( "autocapitalize=\"off\" spellcheck=\"false\" required minlength=\"20\" maxlength=\"512\">" )
				.append( "<button type=\"submit\">Verknüpfen</button></form>" )
				.append( "<p>Der Token wird nur zur Prüfung an Tibber gesendet und anschließend von Amazon gespeichert, " )
				.append( "damit Alexa ihn bei deinen Fragen mitschicken kann. Dieser Dienst speichert ihn nicht. " )
				.append( "Du kannst die Verknüpfung jederzeit in der Alexa-App aufheben und den Token bei Tibber widerrufen.</p>" );
		if ( privacyPolicyUrl != null ) {
			html.append( "<p><a href=\"" ).append( escape( privacyPolicyUrl.toString( ) ) )
					.append( "\" rel=\"noreferrer\">Datenschutzerklärung</a></p>" );
		}
		return html.append( "</body></html>" ).toString( );
	}

	/** KI-generiert: Fehlerseite ohne Formular und ohne reflektierte Eingaben. */
	static String error( String message ) {
		return head( "Verknüpfung nicht möglich" )
		       + "<h1>Verknüpfung nicht möglich</h1><p class=\"error\" role=\"alert\">" + escape( message )
		       + "</p><p>Bitte starte die Verknüpfung erneut aus der Alexa-App.</p></body></html>";
	}

	/** KI-generiert: HTML-Escaping für Text und Attributwerte. */
	static String escape( String value ) {
		StringBuilder escaped = new StringBuilder( value.length( ) );
		for ( char c : value.toCharArray( ) ) {
			switch ( c ) {
				case '&' -> escaped.append( "&amp;" );
				case '<' -> escaped.append( "&lt;" );
				case '>' -> escaped.append( "&gt;" );
				case '"' -> escaped.append( "&quot;" );
				case '\'' -> escaped.append( "&#39;" );
				default -> escaped.append( c );
			}
		}
		return escaped.toString( );
	}

	private static String head( String title ) {
		return "<!DOCTYPE html><html lang=\"de\"><head><meta charset=\"utf-8\">"
		       + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
		       + "<title>" + escape( title ) + "</title><style>" + CSS + "</style></head><body>";
	}

	private static String hidden( String name, String value ) {
		return "<input type=\"hidden\" name=\"" + name + "\" value=\"" + escape( value ) + "\">";
	}

	private static String sha256Base64( String value ) {
		try {
			byte[] digest = MessageDigest.getInstance( "SHA-256" ).digest( value.getBytes( StandardCharsets.UTF_8 ) );
			return Base64.getEncoder( ).encodeToString( digest );
		} catch ( NoSuchAlgorithmException e ) {
			throw new IllegalStateException( "SHA-256 nicht verfügbar", e );
		}
	}
}
