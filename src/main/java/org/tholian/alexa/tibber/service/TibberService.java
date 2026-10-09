package org.tholian.alexa.tibber.service;

import java.io.IOException;
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

	private TibberResponse.PriceInfo priceInfo( String token ) {
		TibberResponse.CurrentSubscription subscription = firstHome( token ).currentSubscription( );
		if ( subscription == null ) {
			throw new TibberApiException( "Kein aktiver Tibber-Vertrag für dieses Zuhause" );
		}
		return subscription.priceInfo( );
	}

	private TibberResponse.Home firstHome( String token ) {
		List<TibberResponse.Home> homes = query( token ).viewer( ).homes( );
		if ( homes.isEmpty( ) ) {
			throw new TibberApiException( "Kein Zuhause im Tibber-Konto hinterlegt" );
		}
		return homes.getFirst( );
	}

	/**
	 * KI-generiert: Führt die GraphQL-Abfrage aus und liefert den {@code data}-Teil.
	 * Wirft {@link TibberApiException} bei Netzwerkfehler/Timeout, HTTP-Status ≠ 200,
	 * GraphQL-Fehlern oder unlesbarer Antwort.
	 */
	private TibberResponse.Data query( String token ) {
		HttpRequest request = HttpRequest.newBuilder( apiUrl )
				.timeout( requestTimeout )
				// SICHERHEIT: Token nur im Header, nie im Log
				.header( "Authorization", "Bearer " + token )
				.header( "Content-Type", "application/json" )
				.POST( HttpRequest.BodyPublishers.ofString( toJson( new GraphQLRequest( PRICE_QUERY ) ) ) )
				.build( );

		HttpResponse<String> response;
		try {
			response = httpClient.send( request, HttpResponse.BodyHandlers.ofString( ) );
		} catch ( IOException e ) {
			// KI-generiert: umfasst auch HttpTimeoutException (Subklasse von IOException)
			throw new TibberApiException( "Tibber API nicht erreichbar: " + e.getClass( ).getSimpleName( ), e );
		} catch ( InterruptedException e ) {
			Thread.currentThread( ).interrupt( );
			throw new TibberApiException( "Tibber-Aufruf unterbrochen", e );
		}

		if ( response.statusCode( ) != 200 ) {
			throw new TibberApiException( "Tibber API antwortet mit HTTP " + response.statusCode( ) );
		}

		TibberResponse body = fromJson( response.body( ) );
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
}
