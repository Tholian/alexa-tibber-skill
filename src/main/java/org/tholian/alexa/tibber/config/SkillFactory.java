package org.tholian.alexa.tibber.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

import com.amazon.ask.CustomSkill;
import com.amazon.ask.builder.CustomSkillBuilder;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.tholian.alexa.tibber.handler.CancelAndStopIntentHandler;
import org.tholian.alexa.tibber.handler.CurrentConsumptionIntentHandler;
import org.tholian.alexa.tibber.handler.CurrentPriceIntentHandler;
import org.tholian.alexa.tibber.handler.FallbackIntentHandler;
import org.tholian.alexa.tibber.handler.GenericExceptionHandler;
import org.tholian.alexa.tibber.handler.HelpIntentHandler;
import org.tholian.alexa.tibber.handler.HighestPriceIntentHandler;
import org.tholian.alexa.tibber.handler.LaunchRequestHandler;
import org.tholian.alexa.tibber.handler.LoggingInterceptor;
import org.tholian.alexa.tibber.handler.LowestPriceIntentHandler;
import org.tholian.alexa.tibber.handler.MDCCleanupInterceptor;
import org.tholian.alexa.tibber.handler.SessionEndedRequestHandler;
import org.tholian.alexa.tibber.handler.UnhandledRequestHandler;
import org.tholian.alexa.tibber.service.TibberService;

/**
 * KI-generiert: Baut den kompletten Skill von Hand zusammen (kein DI-Framework).
 * Wird genau einmal pro Lambda Execution Environment in der Init-Phase aufgerufen.
 * Neue Handler werden ausschließlich in {@link #requestHandlers(TibberService)} ergänzt.
 */
public final class SkillFactory {

	// KI-generiert: Alexa bricht nach 8 s ab – der Tibber-Aufruf muss deutlich darunter bleiben
	static final Duration TIBBER_CONNECT_TIMEOUT = Duration.ofSeconds( 2 );
	static final Duration TIBBER_REQUEST_TIMEOUT = Duration.ofSeconds( 4 );

	private SkillFactory( ) {
	}

	/** KI-generiert: TibberService mit den Skill-Timeouts – gemeinsam für Skill- und Account-Linking-Funktion. */
	public static TibberService tibberService( URI tibberApiUrl ) {
		HttpClient httpClient = HttpClient.newBuilder( )
				.connectTimeout( TIBBER_CONNECT_TIMEOUT )
				.build( );
		return new TibberService( httpClient, new ObjectMapper( ), tibberApiUrl, TIBBER_REQUEST_TIMEOUT, Clock.systemUTC( ) );
	}

	// KI-generiert: Die SDK-Builder-Methoden (addRequestHandlers/-Interceptors) haben generische Varargs-Parameter
	// (z. B. GenericRequestHandler<HandlerInput, Optional<Response>>...); der Compiler muss dafür ein generisches
	// Array anlegen. Das ist hier unbedenklich (nur lesend verwendet) und lässt sich auf Aufruferseite nicht vermeiden.
	@SuppressWarnings( "unchecked" )
	public static CustomSkill create( SkillSettings settings ) {
		TibberService tibberService = tibberService( settings.tibberApiUrl( ) );

		return new CustomSkillBuilder( )
				// SICHERHEIT: Requests mit fremder Skill-ID werden vom ASK SDK abgewiesen
				.withSkillId( settings.skillId( ) )
				.addRequestHandlers( requestHandlers( tibberService ).toArray( RequestHandler[]::new ) )
				// KI-generiert: Reihenfolge ist relevant (erster passender Handler gewinnt) – der Auffang-Handler muss zuletzt kommen
				.addRequestHandlers( new UnhandledRequestHandler( ) )
				.addExceptionHandler( new GenericExceptionHandler( ) )
				.addRequestInterceptors( new LoggingInterceptor( ) )
				.addResponseInterceptors( new MDCCleanupInterceptor( ) )
				.build( );
	}

	/** KI-generiert: Alle Request-Handler des Skills – auch Grundlage für den InteractionModelCoverageTest. */
	static List<RequestHandler> requestHandlers( TibberService tibberService ) {
		return List.of(
				new LaunchRequestHandler( ),
				new CurrentPriceIntentHandler( tibberService ),
				new CurrentConsumptionIntentHandler( tibberService ),
				new LowestPriceIntentHandler( tibberService ),
				new HighestPriceIntentHandler( tibberService ),
				new HelpIntentHandler( ),
				new FallbackIntentHandler( ),
				new CancelAndStopIntentHandler( ),
				new SessionEndedRequestHandler( )
		);
	}
}
