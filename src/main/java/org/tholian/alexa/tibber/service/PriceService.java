package org.tholian.alexa.tibber.service;

import java.util.List;

import org.intellij.lang.annotations.Language;

import org.tholian.alexa.tibber.model.TibberResponse;

/**
 * KI-generiert: Fragt die Preise für heute und morgen in Viertelstunden-Auflösung ab
 * (ohne Argument liefert Tibber nur Stundenwerte).
 * Datensparsamkeit: nur Preisfelder – keine Verbrauchs- oder personenbezogenen Daten.
 */
public class PriceService {

	@Language( "GraphQL" )
	static final String PRICE_QUERY = """
			{
			  viewer {
			    homes {
			      currentSubscription {
			        priceInfo(resolution: QUARTER_HOURLY) {
			          current { total energy tax currency level startsAt }
			          today { total energy tax currency level startsAt }
			          tomorrow { total energy tax currency level startsAt }
			        }
			      }
			    }
			  }
			}
			""";

	private final TibberGraphQlClient client;

	public PriceService( TibberGraphQlClient client ) {
		this.client = client;
	}

	/**
	 * @throws TibberAuthenticationException wenn Tibber den Token ablehnt
	 * @throws TibberApiException            bei Fehlern, ohne Vertrag oder ohne Preisinformationen
	 */
	public PriceSchedule prices( String token ) {
		TibberResponse.Home home = TibberGraphQlClient.firstHome( client.query( token, PRICE_QUERY ) );
		TibberResponse.CurrentSubscription subscription = home.currentSubscription( );
		if ( subscription == null ) {
			throw new TibberApiException( "Kein aktiver Tibber-Vertrag für dieses Zuhause" );
		}
		TibberResponse.PriceInfo priceInfo = subscription.priceInfo( );
		if ( priceInfo == null ) {
			throw new TibberApiException( "Tibber liefert keine Preisinformationen" );
		}
		return PriceSchedule.of( priceInfo.current( ), priceInfo.today( ) == null ? List.of( ) : priceInfo.today( ),
				priceInfo.tomorrow( ) );
	}
}
