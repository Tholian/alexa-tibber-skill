package org.tholian.alexa.tibber.linking;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jspecify.annotations.Nullable;

import lombok.extern.slf4j.Slf4j;

import org.tholian.alexa.tibber.config.SkillFactory;
import org.tholian.alexa.tibber.service.TibberApiException;
import org.tholian.alexa.tibber.service.TibberAuthenticationException;
import org.tholian.alexa.tibber.service.TibberService;

/**
 * KI-generiert: Lambda-Einstieg der Account-Linking-Brücke
 * (Handler: {@code org.tholian.alexa.tibber.linking.AccountLinkingHandler::handleRequest}),
 * angebunden über API Gateway HTTP API (Payload 2.0) unter {@code /link}.
 * <ul>
 *   <li>GET: prüft die OAuth-Parameter von Alexa und zeigt das Formular.</li>
 *   <li>POST: prüft Parameter und Token-Format, verifiziert den Token bei Tibber und leitet mit dem Token im
 *       URL-Fragment an die exakt erlaubte Amazon-Redirect-URL zurück.</li>
 * </ul>
 * SICHERHEIT: Der Token wird weder gespeichert noch geloggt noch in HTML ausgegeben. Es gibt keine
 * Weiterleitung an eine nicht in der Allowlist stehende Adresse.
 */
@Slf4j
public class AccountLinkingHandler implements RequestStreamHandler {

	static final String PATH = "/link";
	static final int MAX_BODY_BYTES = 8 * 1024;
	// KI-generiert: Tibber-Tokens bestehen aus URL-sicheren Zeichen; alles andere ist ein Kopierfehler oder ein Angriff
	private static final Pattern TOKEN_FORMAT = Pattern.compile( "[A-Za-z0-9._~+/=-]{20,512}" );

	private final LinkingSettings settings;
	private final TibberService tibberService;
	private final ObjectMapper objectMapper = new ObjectMapper( );

	/** KI-generiert: Von AWS Lambda verwendeter Konstruktor. */
	public AccountLinkingHandler( ) {
		this( System.getenv( ) );
	}

	/** KI-generiert: Testbarer Konstruktor mit expliziter Umgebung. */
	AccountLinkingHandler( Map<String, String> environment ) {
		this( LinkingSettings.fromEnvironment( environment ) );
	}

	private AccountLinkingHandler( LinkingSettings settings ) {
		this( settings, SkillFactory.tibberService( settings.tibberApiUrl( ) ) );
	}

	/** KI-generiert: Testnaht, um unerwartete Fehler des Tibber-Clients deterministisch auszulösen. */
	AccountLinkingHandler( LinkingSettings settings, TibberService tibberService ) {
		this.settings = settings;
		this.tibberService = tibberService;
	}

	@Override
	public void handleRequest( InputStream input, OutputStream output, @Nullable Context context ) throws IOException {
		JsonNode event = objectMapper.readTree( input );
		objectMapper.writeValue( output, handle( event ).toJson( objectMapper ) );
	}

	HttpResult handle( JsonNode event ) {
		String method = event.path( "requestContext" ).path( "http" ).path( "method" ).asText( "" );
		if ( !PATH.equals( event.path( "rawPath" ).asText( "" ) ) ) {
			return HttpResult.error( 404, "Seite nicht gefunden." );
		}
		try {
			return switch ( method ) {
				case "GET" -> HttpResult.html( 200, LinkingPage.form(
						AuthorizationRequest.validate( queryParameters( event ), settings ), null, settings.privacyPolicyUrl( ) ) );
				case "POST" -> submit( event );
				default -> HttpResult.error( 405, "Methode nicht erlaubt." );
			};
		} catch ( InvalidAuthorizationRequestException e ) {
			// KI-generiert: Message enthält bewusst keine Parameterwerte
			log.warn( "Ungültige Account-Linking-Anfrage: {}", e.getMessage( ) );
			return HttpResult.error( e.status( ), e.getMessage( ) );
		} catch ( RuntimeException e ) {
			// SICHERHEIT: Nur der Klassenname wird geloggt – weder Message noch Objekt noch Stacktrace, da diese
			// Parameterwerte oder den Token enthalten könnten. Der Nutzer erhält trotzdem eine Seite mit Sicherheits-Headern.
			log.error( "Unerwarteter Fehler im Account Linking: {}", e.getClass( ).getSimpleName( ) );
			return HttpResult.error( 500, "Es ist ein interner Fehler aufgetreten. Bitte versuche es später erneut." );
		}
	}

	private HttpResult submit( JsonNode event ) {
		// SICHERHEIT: Das Formular darf nur von dieser Seite selbst abgeschickt werden (Cross-Site-POST abwehren).
		// Auch der Wert "null" (opake/sandboxed Kontexte) stimmt nie mit der erwarteten Origin überein und wird abgewiesen.
		String origin = header( event, "origin" );
		if ( origin != null && !origin.equals( "https://" + header( event, "host" ) ) ) {
			throw new InvalidAuthorizationRequestException( 403, "Anfrage nicht erlaubt." );
		}
		Map<String, String> form = FormData.parse( body( event ) );
		AuthorizationRequest request = AuthorizationRequest.validate( form, settings );

		String token = form.getOrDefault( "tibber_token", "" ).strip( );
		if ( !TOKEN_FORMAT.matcher( token ).matches( ) ) {
			return formWithError( 400, request,
					"Der Token hat ein ungültiges Format. Bitte kopiere ihn vollständig von developer.tibber.com." );
		}
		try {
			tibberService.verifyToken( token );
		} catch ( TibberAuthenticationException e ) {
			// KI-generiert: Die Message enthält nur Status bzw. "UNAUTHENTICATED", nie den Token
			log.info( "Account Linking: Tibber lehnt den Token ab: {}", e.getMessage( ) );
			return formWithError( 400, request, "Tibber hat diesen Token nicht akzeptiert. Bitte prüfe ihn und versuche es erneut." );
		} catch ( TibberApiException e ) {
			// KI-generiert: TibberApiException-Messages enthalten nie den Token
			log.warn( "Account Linking: Token-Prüfung bei Tibber fehlgeschlagen: {}", e.getMessage( ) );
			return formWithError( 502, request,
					"Die Prüfung bei Tibber ist fehlgeschlagen (z. B. kein Zuhause im Konto oder Tibber nicht erreichbar). "
					+ "Bitte versuche es später erneut." );
		}
		log.info( "Account Linking erfolgreich" );
		return HttpResult.redirect( request.successRedirect( token ) );
	}

	private HttpResult formWithError( int status, AuthorizationRequest request, String message ) {
		return HttpResult.html( status, LinkingPage.form( request, message, settings.privacyPolicyUrl( ) ) );
	}

	private static Map<String, String> queryParameters( JsonNode event ) {
		// KI-generiert: Bewusst nicht "queryStringParameters" (API Gateway fasst Duplikate kommagetrennt zusammen und
		// verschleiert so Mehrdeutigkeit), sondern der Roh-String mit strenger Dekodierung; doppelte Parameter ergeben 400.
		// OAuth 2.0 (RFC 6749, Anhang B) verwendet application/x-www-form-urlencoded, daher bedeutet '+' ein Leerzeichen.
		return FormData.parse( event.path( "rawQueryString" ).asText( "" ) );
	}

	private static String body( JsonNode event ) {
		String raw = event.path( "body" ).asText( "" );
		// KI-generiert: Größenprüfung vor dem Dekodieren (Base64 ist ~4/3 der Rohgröße)
		if ( raw.length( ) > MAX_BODY_BYTES * 2 ) {
			throw new InvalidAuthorizationRequestException( 413, "Anfrage zu groß." );
		}
		byte[] bytes;
		try {
			bytes = event.path( "isBase64Encoded" ).asBoolean( false )
			        ? Base64.getDecoder( ).decode( raw )
			        : raw.getBytes( StandardCharsets.UTF_8 );
		} catch ( IllegalArgumentException e ) {
			throw new InvalidAuthorizationRequestException( "Ungültiger Parameter." );
		}
		if ( bytes.length > MAX_BODY_BYTES ) {
			throw new InvalidAuthorizationRequestException( 413, "Anfrage zu groß." );
		}
		return new String( bytes, StandardCharsets.UTF_8 );
	}

	private static @Nullable String header( JsonNode event, String name ) {
		JsonNode value = event.path( "headers" ).path( name );
		return value.isTextual( ) ? value.asText( ) : null;
	}
}
