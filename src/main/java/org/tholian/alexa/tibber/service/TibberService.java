package org.tholian.alexa.tibber.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;

import lombok.extern.slf4j.Slf4j;

import org.tholian.alexa.tibber.model.ConsumptionNode;
import org.tholian.alexa.tibber.model.GraphQLRequest;
import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.model.TibberResponse;

/**
 * KI-generiert: Fragt Preise und Verbrauch bei der Tibber GraphQL API ab und formatiert sie für die Sprachausgabe.
 * <p>
 * HTTP läuft über {@link HttpClient} aus dem JDK – keine zusätzliche Bibliothek, schneller Lambda-Kaltstart.
 * Jeder Aufruf hat einen harten Request-Timeout, damit Alexa (Limit 8 s) immer eine Antwort bekommt.
 * <p>
 * SICHERHEIT: Der Access-Token wird nur als Bearer-Header gesendet und weder geloggt
 * noch in Exception-Messages übernommen.
 */
@Slf4j
public class TibberService {

	private static final ZoneId BERLIN = ZoneId.of( "Europe/Berlin" );
	private static final DateTimeFormatter TIME_FORMATTER =
			DateTimeFormatter.ofPattern( "HH:mm", Locale.GERMAN );

	@Language( "GraphQL" )
	private static final String PRICE_QUERY = """
			{
			  viewer {
			    homes {
			      currentSubscription {
			        priceInfo {
			          current { total currency level startsAt }
			          today { total startsAt level }
			          tomorrow { total startsAt level }
			        }
			      }
			      consumption(resolution: HOURLY, last: 1) {
			        nodes { from to consumption cost }
			      }
			    }
			  }
			}
			""";

	// KI-generiert: Minimalabfrage für die Token-Prüfung beim Account Linking (Datensparsamkeit: nur Home-IDs)
	@Language( "GraphQL" )
	private static final String VERIFY_QUERY = """
			{
			  viewer {
			    homes { id }
			  }
			}
			""";

	private static final String UNAUTHENTICATED = "UNAUTHENTICATED";

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final URI apiUrl;
	private final Duration requestTimeout;
	private final Clock clock;

	/**
	 * @param httpClient     geteilter JDK-HttpClient (Connect-Timeout wird dort gesetzt)
	 * @param objectMapper   Jackson-Mapper für Request/Response
	 * @param apiUrl         Tibber GraphQL Endpoint
	 * @param requestTimeout maximale Dauer eines einzelnen Tibber-Aufrufs
	 * @param clock          Uhr für „jetzt“ (in Tests fest)
	 */
	public TibberService( HttpClient httpClient, ObjectMapper objectMapper, URI apiUrl, Duration requestTimeout, Clock clock ) {
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
		this.apiUrl = apiUrl;
		this.requestTimeout = requestTimeout;
		this.clock = clock;
	}

	public Price getCurrentPrice( String token ) {
		Price current = priceInfo( token ).current( );
		if ( current == null ) {
			throw new TibberApiException( "Tibber liefert keinen aktuellen Preis" );
		}
		return current;
	}

	public List<Price> getPriceList( String token ) {
		TibberResponse.PriceInfo priceInfo = priceInfo( token );
		List<Price> combined = new ArrayList<>( priceInfo.today( ) );
		if ( priceInfo.tomorrow( ) != null ) {
			combined.addAll( priceInfo.tomorrow( ) );
		}
		return combined;
	}

	public Optional<ConsumptionNode> getCurrentConsumption( String token ) {
		TibberResponse.Consumption consumption = firstHome( token ).consumption( );
		if ( consumption == null || consumption.nodes( ) == null || consumption.nodes( ).isEmpty( ) ) {
			return Optional.empty( );
		}
		return Optional.of( consumption.nodes( ).getFirst( ) );
	}

	public Optional<Price> getNextLowestPrice( String token ) {
		OffsetDateTime now = OffsetDateTime.now( clock );
		return getPriceList( token ).stream( )
				.filter( p -> OffsetDateTime.parse( p.startsAt( ) ).isAfter( now ) )
				.min( Comparator.comparingDouble( Price::total ) );
	}

	public Optional<Price> getNextHighestPrice( String token ) {
		OffsetDateTime now = OffsetDateTime.now( clock );
		return getPriceList( token ).stream( )
				.filter( p -> OffsetDateTime.parse( p.startsAt( ) ).isAfter( now ) )
				.max( Comparator.comparingDouble( Price::total ) );
	}

	public String formatPrice( double total ) {
		double cents = total * 100;
		return String.format( Locale.GERMAN, "%.1f Cent", cents );
	}

	public String formatTime( String startsAt ) {
		return OffsetDateTime.parse( startsAt )
					   .atZoneSameInstant( BERLIN )
					   .format( TIME_FORMATTER ) + " Uhr";
	}

	public String formatLevel( @Nullable String level ) {
		if ( level == null ) {
			return "unbekannt";
		}
		return switch ( level ) {
			case "VERY_CHEAP" -> "sehr günstig";
			case "CHEAP" -> "günstig";
			case "NORMAL" -> "normal";
			case "EXPENSIVE" -> "teuer";
			case "VERY_EXPENSIVE" -> "sehr teuer";
			default -> level.toLowerCase( Locale.GERMAN );
		};
	}

	/**
	 * KI-generiert: Prüft beim Account Linking, ob Tibber den Token akzeptiert und mindestens ein Zuhause liefert.
	 *
	 * @throws TibberAuthenticationException wenn Tibber den Token ablehnt
	 * @throws TibberApiException            bei sonstigen Fehlern oder wenn kein Zuhause existiert
	 */
	public void verifyToken( String token ) {
		TibberResponse.Data data = query( token, VERIFY_QUERY );
		// KI-generiert: Unvollständige Antworten (z. B. viewer = null) als Tibber-Fehler melden statt als NullPointerException
		if ( data.viewer( ) == null || data.viewer( ).homes( ) == null ) {
			throw new TibberApiException( "Tibber-Antwort unvollständig" );
		}
		if ( data.viewer( ).homes( ).isEmpty( ) ) {
			throw new TibberApiException( "Kein Zuhause im Tibber-Konto hinterlegt" );
		}
	}

	private TibberResponse.PriceInfo priceInfo( String token ) {
		TibberResponse.CurrentSubscription subscription = firstHome( token ).currentSubscription( );
		if ( subscription == null ) {
			throw new TibberApiException( "Kein aktiver Tibber-Vertrag für dieses Zuhause" );
		}
		return subscription.priceInfo( );
	}

	private TibberResponse.Home firstHome( String token ) {
		List<TibberResponse.Home> homes = query( token, PRICE_QUERY ).viewer( ).homes( );
		if ( homes.isEmpty( ) ) {
			throw new TibberApiException( "Kein Zuhause im Tibber-Konto hinterlegt" );
		}
		return homes.getFirst( );
	}

	/**
	 * KI-generiert: Führt die GraphQL-Abfrage aus und liefert den {@code data}-Teil.
	 * Wirft {@link TibberApiException} bei Netzwerkfehler/Timeout, HTTP-Status ≠ 200,
	 * GraphQL-Fehlern oder unlesbarer Antwort; {@link TibberAuthenticationException} bei abgelehntem Token.
	 */
	private TibberResponse.Data query( String token, String graphQl ) {
		HttpRequest request = HttpRequest.newBuilder( apiUrl )
				.timeout( requestTimeout )
				// SICHERHEIT: Token nur im Header, nie im Log
				.header( "Authorization", "Bearer " + token )
				.header( "Content-Type", "application/json" )
				.POST( HttpRequest.BodyPublishers.ofString( toJson( new GraphQLRequest( graphQl ) ) ) )
				.build( );

		// KI-generiert: HttpRequest.timeout begrenzt nur bis zu den Response-Headern. Damit auch ein langsamer
		// Body den Alexa-Limit nicht sprengt, wird die gesamte Antwort asynchron mit hartem Gesamt-Timeout abgewartet.
		CompletableFuture<HttpResponse<String>> future =
				httpClient.sendAsync( request, HttpResponse.BodyHandlers.ofString( ) );
		HttpResponse<String> response;
		try {
			response = future.get( requestTimeout.toMillis( ), TimeUnit.MILLISECONDS );
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
