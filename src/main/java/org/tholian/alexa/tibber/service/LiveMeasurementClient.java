package org.tholian.alexa.tibber.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;

import org.tholian.alexa.tibber.model.LiveMeasurement;

/**
 * KI-generiert: Holt genau eine Live-Messung (Tibber Pulse) über eine GraphQL-Subscription per WebSocket
 * (Protokoll {@code graphql-transport-ws}) – mit dem WebSocket-Client des JDK, ohne zusätzliche Bibliothek.
 * <p>
 * Ablauf: verbinden → {@code connection_init} (mit Token) → {@code connection_ack} → {@code subscribe}
 * → erste {@code next}-Nachricht mit Messwerten → {@code complete} senden und Verbindung schließen.
 * Alle Schritte laufen innerhalb des übergebenen Zeitbudgets; die Verbindung wird in jedem Fall geschlossen.
 * <p>
 * SICHERHEIT: Der Token steht ausschließlich in der {@code connection_init}-Nachricht. Nachrichteninhalte werden
 * nie geloggt und nie in Exception-Messages übernommen. Die URL muss vorher mit
 * {@code TibberApiUrls.webSocketUrl} geprüft sein, damit der Token nur an Tibber geht.
 */
public class LiveMeasurementClient {

	static final String SUBPROTOCOL = "graphql-transport-ws";
	static final String USER_AGENT = "alexa-tibber-skill/1.0";
	static final Duration MAX_CONNECT_TIMEOUT = Duration.ofSeconds( 2 );
	private static final Duration CLOSE_GRACE = Duration.ofMillis( 200 );
	private static final String SUBSCRIPTION_ID = "1";

	@Language( "GraphQL" )
	static final String SUBSCRIPTION = """
			subscription($homeId: ID!) {
			  liveMeasurement(homeId: $homeId) {
			    timestamp power powerProduction accumulatedConsumption accumulatedCost currency
			  }
			}
			""";

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public LiveMeasurementClient( HttpClient httpClient, ObjectMapper objectMapper ) {
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
	}

	/**
	 * @param url      geprüfte WebSocket-URL von Tibber
	 * @param token    Tibber-Token (nur für {@code connection_init})
	 * @param homeId   Zuhause, dessen Messwerte abonniert werden
	 * @param deadline gemeinsames Zeitbudget des Requests
	 * @throws TibberApiException bei Timeout, Verbindungs- oder Protokollfehler (Message ohne Token)
	 */
	public LiveMeasurement firstMeasurement( URI url, String token, String homeId, Deadline deadline ) {
		Duration connectTimeout = deadline.remainingAtMost( MAX_CONNECT_TIMEOUT );
		if ( connectTimeout.isZero( ) ) {
			throw new TibberApiException( "Live-Daten: kein Zeitbudget mehr" );
		}
		Session session = new Session( token, homeId );
		try {
			httpClient.newWebSocketBuilder( )
					.subprotocols( SUBPROTOCOL )
					.header( "User-Agent", USER_AGENT )
					.connectTimeout( connectTimeout )
					.buildAsync( url, session )
					.get( deadline.remaining( ).toMillis( ), TimeUnit.MILLISECONDS );
			return session.result.get( deadline.remaining( ).toMillis( ), TimeUnit.MILLISECONDS );
		} catch ( TimeoutException e ) {
			throw new TibberApiException( "Live-Daten: Zeitbudget überschritten", e );
		} catch ( ExecutionException e ) {
			Throwable cause = e.getCause( ) != null ? e.getCause( ) : e;
			if ( cause instanceof TibberApiException tibberApiException ) {
				throw tibberApiException;
			}
			throw new TibberApiException( "Live-Daten nicht verfügbar: " + cause.getClass( ).getSimpleName( ), cause );
		} catch ( InterruptedException e ) {
			Thread.currentThread( ).interrupt( );
			throw new TibberApiException( "Live-Abfrage unterbrochen", e );
		} finally {
			session.close( );
		}
	}

	private String json( Object message ) {
		try {
			return objectMapper.writeValueAsString( message );
		} catch ( JsonProcessingException e ) {
			throw new TibberApiException( "Live-Nachricht nicht serialisierbar", e );
		}
	}

	/** KI-generiert: Zustand einer einzelnen Subscription; Callbacks kommen nacheinander vom JDK-WebSocket. */
	private final class Session implements WebSocket.Listener {

		final CompletableFuture<LiveMeasurement> result = new CompletableFuture<>( );
		private final String token;
		private final String homeId;
		private final StringBuilder buffer = new StringBuilder( );
		// KI-generiert: Sendungen dürfen sich beim JDK-WebSocket nicht überlappen – deshalb als Kette
		private CompletableFuture<?> sendChain = CompletableFuture.completedFuture( null );
		private @Nullable WebSocket webSocket;
		private boolean closed;

		Session( String token, String homeId ) {
			this.token = token;
			this.homeId = homeId;
		}

		@Override
		public void onOpen( WebSocket socket ) {
			synchronized ( this ) {
				webSocket = socket;
				if ( closed ) {
					// KI-generiert: Verbindung kam erst nach Ablauf des Budgets zustande – sofort wieder abbrechen
					socket.abort( );
					return;
				}
			}
			// SICHERHEIT: Einzige Stelle, an der der Token gesendet wird
			send( socket, json( Map.of( "type", "connection_init", "payload", Map.of( "token", token ) ) ) );
			socket.request( 1 );
		}

		@Override
		public @Nullable CompletionStage<?> onText( WebSocket socket, CharSequence data, boolean last ) {
			// KI-generiert: Nachrichten können fragmentiert ankommen – erst die vollständige Nachricht auswerten
			buffer.append( data );
			if ( last ) {
				String message = buffer.toString( );
				buffer.setLength( 0 );
				handle( socket, message );
			}
			socket.request( 1 );
			return null;
		}

		@Override
		public @Nullable CompletionStage<?> onClose( WebSocket socket, int statusCode, String reason ) {
			fail( "Verbindung von Tibber geschlossen (Code " + statusCode + ")" );
			return null;
		}

		@Override
		public void onError( WebSocket socket, Throwable error ) {
			result.completeExceptionally(
					new TibberApiException( "Live-Verbindung fehlgeschlagen: " + error.getClass( ).getSimpleName( ), error ) );
		}

		private void handle( WebSocket socket, String message ) {
			JsonNode node;
			try {
				node = objectMapper.readTree( message );
			} catch ( JsonProcessingException e ) {
				fail( "Nachricht nicht lesbar" );
				return;
			}
			switch ( node.path( "type" ).asText( ) ) {
				case "connection_ack" -> send( socket, json( Map.of( "id", SUBSCRIPTION_ID, "type", "subscribe",
						"payload", Map.of( "query", SUBSCRIPTION, "variables", Map.of( "homeId", homeId ) ) ) ) );
				case "ping" -> send( socket, json( Map.of( "type", "pong" ) ) );
				case "next" -> complete( node.path( "payload" ).path( "data" ).path( "liveMeasurement" ) );
				case "error" -> fail( "Tibber meldet einen Fehler in der Subscription" );
				case "complete" -> fail( "Subscription ohne Messwerte beendet" );
				default -> {
					// KI-generiert: andere Nachrichtentypen (z. B. pong) sind für eine einzelne Messung irrelevant
				}
			}
		}

		private void complete( JsonNode measurement ) {
			if ( !measurement.isObject( ) ) {
				fail( "Antwort ohne Messwerte" );
				return;
			}
			try {
				result.complete( objectMapper.treeToValue( measurement, LiveMeasurement.class ) );
			} catch ( JsonProcessingException e ) {
				fail( "Messwerte nicht lesbar" );
			}
		}

		private void fail( String reason ) {
			result.completeExceptionally( new TibberApiException( "Live-Daten: " + reason ) );
		}

		private synchronized void send( WebSocket socket, String text ) {
			sendChain = sendChain.handle( ( ignored, error ) -> null ).thenCompose( ignored -> socket.sendText( text, true ) );
		}

		/** KI-generiert: Subscription beenden und Verbindung schließen – kurz sauber, danach hart. */
		void close( ) {
			WebSocket socket;
			CompletableFuture<?> pending;
			synchronized ( this ) {
				closed = true;
				socket = webSocket;
				if ( socket == null ) {
					return;
				}
				if ( !socket.isOutputClosed( ) ) {
					String completeMessage = json( Map.of( "id", SUBSCRIPTION_ID, "type", "complete" ) );
					sendChain = sendChain.handle( ( ignored, error ) -> null )
							.thenCompose( ignored -> socket.sendText( completeMessage, true ) )
							.thenCompose( ignored -> socket.sendClose( WebSocket.NORMAL_CLOSURE, "" ) );
				}
				pending = sendChain;
			}
			try {
				pending.get( CLOSE_GRACE.toMillis( ), TimeUnit.MILLISECONDS );
			} catch ( ExecutionException | TimeoutException e ) {
				// KI-generiert: best effort – die Verbindung wird im finally ohnehin abgebrochen
			} catch ( InterruptedException e ) {
				Thread.currentThread( ).interrupt( );
			} finally {
				socket.abort( );
			}
		}
	}
}
