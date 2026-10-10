package org.tholian.alexa.tibber.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jspecify.annotations.Nullable;

import lombok.extern.slf4j.Slf4j;

import org.tholian.alexa.tibber.model.GraphQLRequest;
import org.tholian.alexa.tibber.model.TibberResponse;

/**
 * KI-generiert: Transport zur Tibber GraphQL API – HTTP-POST mit Bearer-Token, hartem Gesamt-Timeout und
 * einheitlicher Fehlerabbildung. Die fachlichen Abfragen liegen in den Services (Preise, Energie, Live, Account).
 * <p>
 * SICHERHEIT: Der Access-Token wird nur als Bearer-Header gesendet und weder geloggt noch in
 * Exception-Messages übernommen. Variable Werte (Auflösung, Anzahl, Home-ID) werden ausschließlich als
 * GraphQL-{@code variables} übertragen, nie per String-Verkettung in die Query – keine Injection über Slot-Werte.
 */
@Slf4j
public class TibberGraphQlClient {

	private static final String UNAUTHENTICATED = "UNAUTHENTICATED";

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final URI apiUrl;
	private final Duration requestTimeout;

	/**
	 * @param httpClient     geteilter JDK-HttpClient (Connect-Timeout wird dort gesetzt; auch Basis für WebSockets)
	 * @param objectMapper   Jackson-Mapper für Request/Response
	 * @param apiUrl         Tibber GraphQL Endpoint
	 * @param requestTimeout Standard-Höchstdauer eines einzelnen Tibber-Aufrufs
	 */
	public TibberGraphQlClient( HttpClient httpClient, ObjectMapper objectMapper, URI apiUrl, Duration requestTimeout ) {
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
		this.apiUrl = apiUrl;
		this.requestTimeout = requestTimeout;
	}

	public HttpClient httpClient( ) {
		return httpClient;
	}

	public ObjectMapper objectMapper( ) {
		return objectMapper;
	}

	public Duration requestTimeout( ) {
		return requestTimeout;
	}

	/** KI-generiert: Abfrage ohne Variablen mit Standard-Timeout. */
	public TibberResponse.Data query( String token, String graphQl ) {
		return query( token, graphQl, null, requestTimeout );
	}

	/** KI-generiert: Abfrage mit GraphQL-Variablen und Standard-Timeout. */
	public TibberResponse.Data query( String token, String graphQl, @Nullable Map<String, Object> variables ) {
		return query( token, graphQl, variables, requestTimeout );
	}

	/**
	 * KI-generiert: Führt die GraphQL-Abfrage aus und liefert den {@code data}-Teil.
	 * Wirft {@link TibberApiException} bei Netzwerkfehler/Timeout, HTTP-Status ≠ 200,
	 * GraphQL-Fehlern oder unlesbarer Antwort; {@link TibberAuthenticationException} bei abgelehntem Token.
	 *
	 * @param timeout Gesamt-Timeout dieses Aufrufs (z. B. Rest eines Zeitbudgets); muss positiv sein
	 */
	public TibberResponse.Data query( String token, String graphQl, @Nullable Map<String, Object> variables, Duration timeout ) {
		// KI-generiert: HttpRequest.timeout akzeptiert keine Null-Dauer – ein aufgebrauchtes Zeitbudget gilt als Timeout
		if ( timeout.isZero( ) || timeout.isNegative( ) ) {
			throw new TibberApiException( "Tibber API nicht erreichbar: Zeitbudget aufgebraucht" );
		}
		HttpRequest request = HttpRequest.newBuilder( apiUrl )
				.timeout( timeout )
				// SICHERHEIT: Token nur im Header, nie im Log
				.header( "Authorization", "Bearer " + token )
				.header( "Content-Type", "application/json" )
				.POST( HttpRequest.BodyPublishers.ofString( toJson( new GraphQLRequest( graphQl, variables ) ) ) )
				.build( );

		// KI-generiert: HttpRequest.timeout begrenzt nur bis zu den Response-Headern. Damit auch ein langsamer
		// Body das Alexa-Limit nicht sprengt, wird die gesamte Antwort asynchron mit hartem Gesamt-Timeout abgewartet.
		CompletableFuture<HttpResponse<String>> future =
				httpClient.sendAsync( request, HttpResponse.BodyHandlers.ofString( ) );
		HttpResponse<String> response;
		try {
			response = future.get( timeout.toMillis( ), TimeUnit.MILLISECONDS );
		} catch ( TimeoutException e ) {
			future.cancel( true );
			throw new TibberApiException( "Tibber API nicht erreichbar: TimeoutException", e );
		} catch ( ExecutionException e ) {
			// KI-generiert: Ursache auspacken (z. B. ConnectException, HttpTimeoutException) für eine aussagekräftige Meldung
			Throwable cause = e.getCause( ) != null ? e.getCause( ) : e;
			throw new TibberApiException( "Tibber API nicht erreichbar: " + cause.getClass( ).getSimpleName( ), cause );
		} catch ( InterruptedException e ) {
			future.cancel( true );
			Thread.currentThread( ).interrupt( );
			throw new TibberApiException( "Tibber-Aufruf unterbrochen", e );
		}

		int status = response.statusCode( );
		// SICHERHEIT: Abgelehnter Token → eigene Exception, damit der Skill zur Neuverknüpfung auffordert
		if ( status == 401 || status == 403 ) {
			throw new TibberAuthenticationException( "Tibber lehnt den Access-Token ab (HTTP " + status + ")" );
		}
		@Nullable TibberResponse parsed = status == 200 ? fromJson( response.body( ) ) : tryParse( response.body( ) );
		if ( parsed != null && isUnauthenticated( parsed ) ) {
			throw new TibberAuthenticationException( "Tibber lehnt den Access-Token ab (UNAUTHENTICATED)" );
		}
		if ( status != 200 || parsed == null ) {
			throw new TibberApiException( "Tibber API antwortet mit HTTP " + status );
		}
		TibberResponse body = parsed;
		if ( body.errors( ) != null && !body.errors( ).isEmpty( ) ) {
			String messages = body.errors( ).stream( )
					.map( error -> String.valueOf( error.message( ) ) )
					.collect( Collectors.joining( "; " ) );
			throw new TibberApiException( "Tibber GraphQL-Fehler: " + messages );
		}
		if ( body.data( ) == null ) {
			throw new TibberApiException( "Tibber-Antwort enthält keine Daten" );
		}
		log.debug( "Tibber-Abfrage erfolgreich" );
		return body.data( );
	}

	/**
	 * KI-generiert: Erstes Zuhause des Kontos. Mehrere Zuhause werden bewusst nicht unterstützt (siehe BACKLOG).
	 *
	 * @throws TibberApiException bei unvollständiger Antwort oder wenn kein Zuhause existiert
	 */
	public static TibberResponse.Home firstHome( TibberResponse.Data data ) {
		TibberResponse.Viewer viewer = data.viewer( );
		// KI-generiert: Unvollständige Antworten (z. B. viewer = null) als Tibber-Fehler melden statt als NullPointerException
		if ( viewer == null || viewer.homes( ) == null ) {
			throw new TibberApiException( "Tibber-Antwort unvollständig" );
		}
		List<TibberResponse.Home> homes = viewer.homes( );
		if ( homes.isEmpty( ) ) {
			throw new TibberApiException( "Kein Zuhause im Tibber-Konto hinterlegt" );
		}
		return homes.getFirst( );
	}

	private String toJson( GraphQLRequest request ) {
		try {
			return objectMapper.writeValueAsString( request );
		} catch ( JsonProcessingException e ) {
			throw new TibberApiException( "GraphQL-Request nicht serialisierbar", e );
		}
	}

	private TibberResponse fromJson( String json ) {
		try {
			return objectMapper.readValue( json, TibberResponse.class );
		} catch ( JsonProcessingException e ) {
			throw new TibberApiException( "Tibber-Antwort nicht lesbar", e );
		}
	}

	/** KI-generiert: Wie {@link #fromJson(String)}, liefert aber {@code null} statt einer Exception (Fehlerantworten). */
	private @Nullable TibberResponse tryParse( String json ) {
		try {
			return objectMapper.readValue( json, TibberResponse.class );
		} catch ( JsonProcessingException e ) {
			return null;
		}
	}

	private static boolean isUnauthenticated( TibberResponse response ) {
		return response.errors( ) != null && response.errors( ).stream( )
				.anyMatch( error -> error.extensions( ) != null && UNAUTHENTICATED.equals( error.extensions( ).code( ) ) );
	}
}
