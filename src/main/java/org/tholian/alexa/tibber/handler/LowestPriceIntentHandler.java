package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;

import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.service.TibberService;

@Component
public class LowestPriceIntentHandler implements RequestHandler {

	private static final Logger log = LoggerFactory.getLogger( LowestPriceIntentHandler.class );

	private final TibberService tibberService;

	public LowestPriceIntentHandler( TibberService tibberService ) {
		this.tibberService = tibberService;
	}

	@Override
	public boolean canHandle( HandlerInput input ) {
		return input.matches( Predicates.intentName( "GetLowestPriceIntent" ) );
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
			Optional<Price> lowestPrice = tibberService.getNextLowestPrice( token );
			if ( lowestPrice.isEmpty( ) ) {
				return input.getResponseBuilder( )
						.withSpeech( "Es sind keine Preisdaten für die nächsten Stunden verfügbar." )
						.build( );
			}
			Price price = lowestPrice.get( );
			String speech = "Der günstigste Strompreis in den nächsten 24 Stunden ist um "
			                + tibberService.formatTime( price.startsAt( ) )
			                + " mit " + tibberService.formatPrice( price.total( ) ) + " pro Kilowattstunde.";
			return input.getResponseBuilder( )
					.withSpeech( speech )
					.build( );
		} catch ( Exception e ) {
			log.error( "Fehler beim Abrufen des Tiefstpreises", e );
			return input.getResponseBuilder( )
					.withSpeech( "Es gibt gerade ein Problem beim Abrufen der Preisdaten. Bitte versuche es später noch einmal." )
					.build( );
		}
	}
}
