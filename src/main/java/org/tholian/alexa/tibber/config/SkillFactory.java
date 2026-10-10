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
import org.tholian.alexa.tibber.handler.CheapestWindowIntentHandler;
import org.tholian.alexa.tibber.handler.ConsumptionForPeriodIntentHandler;
import org.tholian.alexa.tibber.handler.CurrentConsumptionIntentHandler;
import org.tholian.alexa.tibber.handler.CurrentPriceIntentHandler;
import org.tholian.alexa.tibber.handler.DayPriceSummaryIntentHandler;
import org.tholian.alexa.tibber.handler.FallbackIntentHandler;
import org.tholian.alexa.tibber.handler.GenericExceptionHandler;
import org.tholian.alexa.tibber.handler.HelpIntentHandler;
import org.tholian.alexa.tibber.handler.HighestPriceIntentHandler;
import org.tholian.alexa.tibber.handler.IsPriceCheapIntentHandler;
import org.tholian.alexa.tibber.handler.LaunchRequestHandler;
import org.tholian.alexa.tibber.handler.LoggingInterceptor;
import org.tholian.alexa.tibber.handler.LowestPriceIntentHandler;
import org.tholian.alexa.tibber.handler.MDCCleanupInterceptor;
import org.tholian.alexa.tibber.handler.PriceAtTimeIntentHandler;
import org.tholian.alexa.tibber.handler.PriceBreakdownIntentHandler;
import org.tholian.alexa.tibber.handler.ProductionForPeriodIntentHandler;
import org.tholian.alexa.tibber.handler.SessionEndedRequestHandler;
import org.tholian.alexa.tibber.handler.UnhandledRequestHandler;
import org.tholian.alexa.tibber.service.TibberAccountService;
import org.tholian.alexa.tibber.service.TibberGraphQlClient;

/**
 * KI-generiert: Baut den kompletten Skill von Hand zusammen (kein DI-Framework).
 * Wird genau einmal pro Lambda Execution Environment in der Init-Phase aufgerufen.
 * Neue Handler werden ausschließlich in {@link #requestHandlers(SkillServices)} ergänzt.
 */
public final class SkillFactory {

	// KI-generiert: Alexa bricht nach 8 s ab – der Tibber-Aufruf muss deutlich darunter bleiben
	static final Duration TIBBER_CONNECT_TIMEOUT = Duration.ofSeconds( 2 );
	static final Duration TIBBER_REQUEST_TIMEOUT = Duration.ofSeconds( 4 );

	// KI-generiert: Live-Daten (HTTP-Vorabfrage + WebSocket) – 2 s Puffer bis zum 8-s-Limit von Alexa und Lambda
	static final Duration LIVE_BUDGET = Duration.ofSeconds( 6 );

	private SkillFactory( ) {
	}

	/** KI-generiert: GraphQL-Client mit den Skill-Timeouts – gemeinsam für Skill- und Account-Linking-Funktion. */
	public static TibberGraphQlClient tibberClient( URI tibberApiUrl ) {
		HttpClient httpClient = HttpClient.newBuilder( )
				.connectTimeout( TIBBER_CONNECT_TIMEOUT )
				.build( );
		return new TibberGraphQlClient( httpClient, new ObjectMapper( ), tibberApiUrl, TIBBER_REQUEST_TIMEOUT );
	}

	/** KI-generiert: Token-Prüfung für die Account-Linking-Funktion. */
	public static TibberAccountService tibberAccountService( URI tibberApiUrl ) {
		return new TibberAccountService( tibberClient( tibberApiUrl ) );
	}

	// KI-generiert: Die SDK-Builder-Methoden (addRequestHandlers/-Interceptors) haben generische Varargs-Parameter
	// (z. B. GenericRequestHandler<HandlerInput, Optional<Response>>...); der Compiler muss dafür ein generisches
	// Array anlegen. Das ist hier unbedenklich (nur lesend verwendet) und lässt sich auf Aufruferseite nicht vermeiden.
	@SuppressWarnings( "unchecked" )
	public static CustomSkill create( SkillSettings settings ) {
		SkillServices services = SkillServices.create( settings.tibberApiUrl( ), Clock.systemUTC( ) );

		return new CustomSkillBuilder( )
				// SICHERHEIT: Requests mit fremder Skill-ID werden vom ASK SDK abgewiesen
				.withSkillId( settings.skillId( ) )
				.addRequestHandlers( requestHandlers( services ).toArray( RequestHandler[]::new ) )
				// KI-generiert: Reihenfolge ist relevant (erster passender Handler gewinnt) – der Auffang-Handler muss zuletzt kommen
				.addRequestHandlers( new UnhandledRequestHandler( ) )
				.addExceptionHandler( new GenericExceptionHandler( ) )
				.addRequestInterceptors( new LoggingInterceptor( ) )
				.addResponseInterceptors( new MDCCleanupInterceptor( ) )
				.build( );
	}

	/** KI-generiert: Alle spezifischen Request-Handler des Skills – auch Grundlage für den InteractionModelCoverageTest. */
	static List<RequestHandler> requestHandlers( SkillServices services ) {
		return List.of(
				new LaunchRequestHandler( ),
				new CurrentPriceIntentHandler( services.prices( ), services.formatter( ) ),
				new CurrentConsumptionIntentHandler( services.live( ), services.formatter( ), LIVE_BUDGET ),
				new LowestPriceIntentHandler( services.prices( ), services.formatter( ), services.clock( ) ),
				new HighestPriceIntentHandler( services.prices( ), services.formatter( ), services.clock( ) ),
				new PriceAtTimeIntentHandler( services.prices( ), services.formatter( ), services.clock( ) ),
				new DayPriceSummaryIntentHandler( services.prices( ), services.formatter( ), services.clock( ) ),
				new CheapestWindowIntentHandler( services.prices( ), services.formatter( ), services.clock( ) ),
				new IsPriceCheapIntentHandler( services.prices( ), services.formatter( ), services.clock( ) ),
				new PriceBreakdownIntentHandler( services.prices( ), services.formatter( ) ),
				new ConsumptionForPeriodIntentHandler( services.energy( ), services.formatter( ) ),
				new ProductionForPeriodIntentHandler( services.energy( ), services.formatter( ) ),
				new HelpIntentHandler( ),
				new FallbackIntentHandler( ),
				new CancelAndStopIntentHandler( ),
				new SessionEndedRequestHandler( )
		);
	}
}
