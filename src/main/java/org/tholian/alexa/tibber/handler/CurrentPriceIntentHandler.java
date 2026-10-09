

package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;

import lombok.extern.slf4j.Slf4j;
import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.service.TibberAuthenticationException;
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
		String token = AccountLinkingResponses.accessToken( input );
		if ( token == null ) {
			return AccountLinkingResponses.linkAccount( input );
		}
		try {
			Price price = tibberService.getCurrentPrice( token );
			String speech = "Der aktuelle Strompreis beträgt " + tibberService.formatPrice( price.total( ) )
			                + " pro Kilowattstunde. Das Preisniveau ist " + tibberService.formatLevel( price.level( ) ) + ".";
			return input.getResponseBuilder( )
					.withSpeech( speech )
					.build( );
		} catch ( TibberAuthenticationException e ) {
			// KI-generiert: Token bei Tibber widerrufen/ungültig → Neuverknüpfung statt allgemeiner Fehlermeldung
			log.warn( "Tibber lehnt den Access-Token ab – Neuverknüpfung angefordert" );
			return AccountLinkingResponses.relinkAccount( input );
		} catch ( Exception e ) {
			log.error( "Fehler beim Abrufen des Strompreises", e );
			return input.getResponseBuilder( )
					.withSpeech( "Es gibt gerade ein Problem beim Abrufen des Strompreises. Bitte versuche es später noch einmal." )
					.build( );
		}
	}
}
