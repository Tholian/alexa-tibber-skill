package org.tholian.alexa.tibber.config;

import com.amazon.ask.CustomSkill;
import com.amazon.ask.Skills;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import org.tholian.alexa.tibber.handler.CancelAndStopIntentHandler;
import org.tholian.alexa.tibber.handler.CurrentConsumptionIntentHandler;
import org.tholian.alexa.tibber.handler.CurrentPriceIntentHandler;
import org.tholian.alexa.tibber.handler.FallbackIntentHandler;
import org.tholian.alexa.tibber.handler.HelpIntentHandler;
import org.tholian.alexa.tibber.handler.HighestPriceIntentHandler;
import org.tholian.alexa.tibber.handler.LaunchRequestHandler;
import org.tholian.alexa.tibber.handler.LowestPriceIntentHandler;
import org.tholian.alexa.tibber.handler.SessionEndedRequestHandler;

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
			SessionEndedRequestHandler sessionEndedRequestHandler
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
				.build( );
	}

	@Bean
	public RestClient restClient( @Value( "${tibber.api-url}" ) String apiUrl ) {
		return RestClient.builder( )
				.baseUrl( apiUrl )
				.defaultHeader( "Content-Type", MediaType.APPLICATION_JSON_VALUE )
				.build( );
	}
}
