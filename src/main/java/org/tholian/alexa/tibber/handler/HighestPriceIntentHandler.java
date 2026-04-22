

package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.service.TibberService;

@Slf4j
@Component
@RequiredArgsConstructor
public class HighestPriceIntentHandler implements RequestHandler {

	private final TibberService tibberService;

	@Override
	public boolean canHandle( HandlerInput input ) {
		return input.matches( Predicates.intentName( "GetHighestPriceIntent" ) );
	}

	@Override
	public Optional<Response> handle( HandlerInput input ) {
		String token = input.getRequestEnvelope( ).getContext( ).getSystem( ).getUser( ).getAccessToken( );
		if ( token == null ) {
			return input.getResponseBuilder( )
					.withSpeech( "Bitte verknüpfe zunächst deinen Tibber Account in der Alexa App." )
					.withLinkAccountCard( )
					.build( );
		}
		try {
			Optional<Price> highestPrice = tibberService.getNextHighestPrice( token );
			if ( highestPrice.isEmpty( ) ) {
				return input.getResponseBuilder( )
						.withSpeech( "Es sind keine Preisdaten für die nächsten Stunden verfügbar." )
						.build( );
			}
			Price price = highestPrice.get( );
			String speech = "Der teuerste Strompreis in den nächsten 24 Stunden ist um "
			                + tibberService.formatTime( price.startsAt( ) )
			                + " mit " + tibberService.formatPrice( price.total( ) ) + " pro Kilowattstunde.";
			return input.getResponseBuilder( )
					.withSpeech( speech )
					.build( );
		} catch ( Exception e ) {
			log.error( "Fehler beim Abrufen des Höchstpreises", e );
			return input.getResponseBuilder( )
					.withSpeech( "Es gibt gerade ein Problem beim Abrufen der Preisdaten. Bitte versuche es später noch einmal." )
					.build( );
		}
	}
}
