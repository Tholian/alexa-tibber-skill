package org.tholian.alexa.tibber.service;

import java.net.URI;
import java.util.Optional;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;

import lombok.extern.slf4j.Slf4j;

import org.tholian.alexa.tibber.config.TibberApiUrls;
import org.tholian.alexa.tibber.model.ConsumptionNode;
import org.tholian.alexa.tibber.model.TibberResponse;

/**
 * KI-generiert: „Wie viel verbrauche ich gerade?“ – Live-Messung, wenn das Zuhause Live-Daten liefert,
 * sonst die letzte abgerechnete Stunde. Eine HTTP-Vorabfrage liefert WebSocket-URL, Home-ID, Pulse-Flag
 * und zugleich die Daten für den Rückfall; beide Aufrufe teilen sich ein Zeitbudget ({@link Deadline}).
 * <p>
 * Live-Daten sind optional: Timeout, Verbindungs- oder Protokollfehler führen zum Rückfall statt zu einer
 * Fehlermeldung. Nur ein abgelehnter Token (Vorabfrage) wird weitergereicht.
 * <p>
 * SICHERHEIT: Die WebSocket-URL stammt aus der API-Antwort und wird vor der Verbindung geprüft – nur Tibber-Hosts
 * (bzw. Loopback in Tests) erhalten den Token. Log-Meldungen enthalten keine Nachrichteninhalte und keinen Token.
 */
@Slf4j
public class LiveService {

	@Language( "GraphQL" )
	static final String PREREQUISITE_QUERY = """
			{
			  viewer {
			    websocketSubscriptionUrl
			    homes {
			      id
			      features { realTimeConsumptionEnabled }
			      consumption(resolution: HOURLY, last: 1) {
			        nodes { from to consumption cost currency }
			      }
			    }
			  }
			}
			""";

	private final TibberGraphQlClient client;
	private final LiveMeasurementClient liveClient;

	public LiveService( TibberGraphQlClient client, LiveMeasurementClient liveClient ) {
		this.client = client;
		this.liveClient = liveClient;
	}

	/**
	 * @throws TibberAuthenticationException wenn Tibber den Token ablehnt
	 * @throws TibberApiException            wenn bereits die Vorabfrage scheitert
	 */
	public CurrentUsage currentUsage( String token, Deadline deadline ) {
		TibberResponse.Data data = client.query( token, PREREQUISITE_QUERY, null, deadline.remainingAtMost( client.requestTimeout( ) ) );
		TibberResponse.Home home = TibberGraphQlClient.firstHome( data );
		CurrentUsage.LastHour fallback = new CurrentUsage.LastHour( lastHour( home ) );
		boolean liveEnabled = home.features( ) != null && Boolean.TRUE.equals( home.features( ).realTimeConsumptionEnabled( ) );
		@Nullable String homeId = home.id( );
		if ( !liveEnabled || homeId == null ) {
			return fallback;
		}
		@Nullable String rawUrl = data.viewer( ) == null ? null : data.viewer( ).websocketSubscriptionUrl( );
		Optional<URI> url = TibberApiUrls.webSocketUrl( rawUrl );
		if ( url.isEmpty( ) ) {
			log.warn( "Tibber meldet keine zulässige WebSocket-URL – Live-Daten übersprungen" );
			return fallback;
		}
		try {
			return new CurrentUsage.Live( liveClient.firstMeasurement( url.get( ), token, homeId, deadline ) );
		} catch ( TibberApiException e ) {
			// KI-generiert: TibberApiException-Messages enthalten nie den Token
			log.warn( "Live-Daten nicht verfügbar, Rückfall auf die letzte Stunde: {}", e.getMessage( ) );
			return fallback;
		}
	}

	private static @Nullable ConsumptionNode lastHour( TibberResponse.Home home ) {
		TibberResponse.Consumption consumption = home.consumption( );
		if ( consumption == null || consumption.nodes( ) == null || consumption.nodes( ).isEmpty( ) ) {
			return null;
		}
		return consumption.nodes( ).getFirst( );
	}
}
