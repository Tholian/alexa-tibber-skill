package org.tholian.alexa.tibber.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tholian.alexa.tibber.model.ConsumptionNode;
import org.tholian.alexa.tibber.model.GraphQLRequest;
import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.model.TibberResponse;

@Service
public class TibberService {

	private static final Logger log = LoggerFactory.getLogger( TibberService.class );
	private static final ZoneId BERLIN = ZoneId.of( "Europe/Berlin" );
	private static final DateTimeFormatter TIME_FORMATTER =
			DateTimeFormatter.ofPattern( "HH:mm", Locale.GERMAN );

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
			        nodes { from to consumption unit cost }
			      }
			    }
			  }
			}
			""";

	private final RestClient restClient;

	public TibberService( RestClient restClient ) {
		this.restClient = restClient;
	}

	public Price getCurrentPrice( String token ) {
		TibberResponse response = query( token );
		return response.data( ).viewer( ).homes( ).get( 0 )
				.currentSubscription( ).priceInfo( ).current( );
	}

	public List<Price> getPriceList( String token ) {
		TibberResponse response = query( token );
		var priceInfo = response.data( ).viewer( ).homes( ).get( 0 )
				.currentSubscription( ).priceInfo( );
		List<Price> combined = new ArrayList<>( priceInfo.today( ) );
		if ( priceInfo.tomorrow( ) != null ) {
			combined.addAll( priceInfo.tomorrow( ) );
		}
		return combined;
	}

	public Optional<ConsumptionNode> getCurrentConsumption( String token ) {
		TibberResponse response = query( token );
		var nodes = response.data( ).viewer( ).homes( ).get( 0 )
				.consumption( ).nodes( );
		if ( nodes == null || nodes.isEmpty( ) ) {
			return Optional.empty( );
		}
		return Optional.of( nodes.get( 0 ) );
	}

	public Optional<Price> getNextLowestPrice( String token ) {
		OffsetDateTime now = OffsetDateTime.now( BERLIN );
		return getPriceList( token ).stream( )
				.filter( p -> OffsetDateTime.parse( p.startsAt( ) ).isAfter( now ) )
				.min( Comparator.comparingDouble( Price::total ) );
	}

	public Optional<Price> getNextHighestPrice( String token ) {
		OffsetDateTime now = OffsetDateTime.now( BERLIN );
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

	public String formatLevel( String level ) {
		return switch ( level ) {
			case "VERY_CHEAP" -> "sehr günstig";
			case "CHEAP" -> "günstig";
			case "NORMAL" -> "normal";
			case "EXPENSIVE" -> "teuer";
			case "VERY_EXPENSIVE" -> "sehr teuer";
			default -> level.toLowerCase( Locale.GERMAN );
		};
	}

	private TibberResponse query( String token ) {
		return restClient.post( )
				.header( "Authorization", "Bearer " + token )
				.body( new GraphQLRequest( PRICE_QUERY ) )
				.retrieve( )
				.body( TibberResponse.class );
	}
}
