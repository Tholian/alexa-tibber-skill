

package org.tholian.alexa.tibber.handler;

import java.util.Locale;
import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;

import lombok.extern.slf4j.Slf4j;
import org.tholian.alexa.tibber.model.ConsumptionNode;
import org.tholian.alexa.tibber.service.TibberAuthenticationException;
import org.tholian.alexa.tibber.service.TibberService;

@Slf4j
public class CurrentConsumptionIntentHandler implements RequestHandler {

	private final TibberService tibberService;

	public CurrentConsumptionIntentHandler( TibberService tibberService ) {
		this.tibberService = tibberService;
	}

	@Override
	public boolean canHandle( HandlerInput input ) {
		return input.matches( Predicates.intentName( "GetCurrentConsumptionIntent" ) );
	}

	@Override
	public Optional<Response> handle( HandlerInput input ) {
		String token = AccountLinkingResponses.accessToken( input );
		if ( token == null ) {
			return AccountLinkingResponses.linkAccount( input );
		}
		try {
			Optional<ConsumptionNode> node = tibberService.getCurrentConsumption( token );
			if ( node.isEmpty( ) ) {
				return input.getResponseBuilder( )
						.withSpeech( "Es sind noch keine Verbrauchsdaten verfügbar." )
						.build( );
			}
			ConsumptionNode consumption = node.get( );
			String from = tibberService.formatTime( consumption.from( ) );
			String speech;
			if ( consumption.consumption( ) != null && consumption.cost( ) != null ) {
				speech = String.format( Locale.GERMAN,
						"In der Stunde ab %s hast du %.2f Kilowattstunden verbraucht und dafür %.2f Euro bezahlt.",
						from, consumption.consumption( ), consumption.cost( ) );
			} else {
				speech = "In der Stunde ab " + from + " sind noch keine vollständigen Verbrauchsdaten verfügbar.";
			}
			return input.getResponseBuilder( )
					.withSpeech( speech )
					.build( );
		} catch ( TibberAuthenticationException e ) {
			// KI-generiert: Token bei Tibber widerrufen/ungültig → Neuverknüpfung statt allgemeiner Fehlermeldung
			// KI-generiert: Die Message enthält nur Status bzw. "UNAUTHENTICATED", nie den Token
			log.warn( "Tibber lehnt den Access-Token ab – Neuverknüpfung angefordert: {}", e.getMessage( ) );
			return AccountLinkingResponses.relinkAccount( input );
		} catch ( Exception e ) {
			log.error( "Fehler beim Abrufen des Verbrauchs", e );
			return input.getResponseBuilder( )
					.withSpeech( "Es gibt gerade ein Problem beim Abrufen der Verbrauchsdaten. Bitte versuche es später noch einmal." )
					.build( );
		}
	}
}
