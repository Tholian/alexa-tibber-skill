
package org.tholian.alexa.tibber.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

import com.amazon.ask.CustomSkill;
import com.amazon.ask.Skills;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.tholian.alexa.tibber.handler.CancelAndStopIntentHandler;
import org.tholian.alexa.tibber.handler.CurrentConsumptionIntentHandler;
import org.tholian.alexa.tibber.handler.CurrentPriceIntentHandler;
import org.tholian.alexa.tibber.handler.FallbackIntentHandler;
import org.tholian.alexa.tibber.handler.HelpIntentHandler;
import org.tholian.alexa.tibber.handler.HighestPriceIntentHandler;
import org.tholian.alexa.tibber.handler.LaunchRequestHandler;
import org.tholian.alexa.tibber.handler.LoggingInterceptor;
import org.tholian.alexa.tibber.handler.LowestPriceIntentHandler;
import org.tholian.alexa.tibber.handler.MDCCleanupInterceptor;
import org.tholian.alexa.tibber.handler.SessionEndedRequestHandler;
import org.tholian.alexa.tibber.service.TibberService;

@Configuration
public class SkillConfig {

	@Bean
	public CustomSkill skill(
			LaunchRequestHandler launchRequestHandler,
			CurrentPriceIntentHandler currentPriceIntentHandler,
			CurrentConsumptionIntentHandler currentConsumptionIntentHandler,
			LowestPriceIntentHandler lowestPriceIntentHandler,
			HighestPriceIntentHandler highestPriceIntentHandler,
			HelpIntentHandler helpIntentHandler,
			FallbackIntentHandler fallbackIntentHandler,
			CancelAndStopIntentHandler cancelAndStopIntentHandler,
			SessionEndedRequestHandler sessionEndedRequestHandler,
			// KI-generiert: Interceptors für MDC-Logging
			LoggingInterceptor loggingInterceptor,
			MDCCleanupInterceptor mdcCleanupInterceptor
	) {
		return Skills.custom( )
				.addRequestHandlers(
						launchRequestHandler,
						currentPriceIntentHandler,
						currentConsumptionIntentHandler,
						lowestPriceIntentHandler,
						highestPriceIntentHandler,
						helpIntentHandler,
						fallbackIntentHandler,
						cancelAndStopIntentHandler,
						sessionEndedRequestHandler
				)
				.addRequestInterceptors( loggingInterceptor )
				.addResponseInterceptors( mdcCleanupInterceptor )
				.build( );
	}

	// KI-generiert: Übergangslösung bis Task 3 – die Datei wird dort gelöscht
	@Bean
	public TibberService tibberService( @Value( "${tibber.api-url}" ) String apiUrl ) {
		return new TibberService( HttpClient.newHttpClient( ), new ObjectMapper( ), URI.create( apiUrl ),
				Duration.ofSeconds( 4 ), Clock.systemUTC( ) );
	}
}
