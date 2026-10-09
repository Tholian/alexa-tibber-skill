

package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;

import lombok.extern.slf4j.Slf4j;
import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.service.TibberService;

@Slf4j
public class CurrentPriceIntentHandler implements RequestHandler {

	private final TibberService tibberService;

	public CurrentPriceIntentHandler( TibberService tibberService ) {
		this.tibberService = tibberService;
	}

	@Override
	public boolean canHandle( HandlerInput input ) {
		return input.matches( Predicates.intentName( "GetCurrentPriceIntent" ) );
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
			Price price = tibberService.getCurrentPrice( token );
			String speech = "Der aktuelle Strompreis beträgt " + tibberService.formatPrice( price.total( ) )
			                + " pro Kilowattstunde. Das Preisniveau ist " + tibberService.formatLevel( price.level( ) ) + ".";
			return input.getResponseBuilder( )
					.withSpeech( speech )
					.build( );
		} catch ( Exception e ) {
			log.error( "Fehler beim Abrufen des Strompreises", e );
			return input.getResponseBuilder( )
					.withSpeech( "Es gibt gerade ein Problem beim Abrufen des Strompreises. Bitte versuche es später noch einmal." )
					.build( );
		}
	}
}
