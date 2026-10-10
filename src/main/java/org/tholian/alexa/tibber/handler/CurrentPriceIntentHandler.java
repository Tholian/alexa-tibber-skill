package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;

import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.service.PriceSchedule;
import org.tholian.alexa.tibber.service.PriceService;
import org.tholian.alexa.tibber.service.SpeechFormatter;
import org.tholian.alexa.tibber.service.TibberApiException;

/**
 * KI-generiert: „Was kostet Strom gerade?“ – Preis der laufenden Viertelstunde und Preisniveau.
 */
public class CurrentPriceIntentHandler extends TibberIntentHandler {

	private final PriceService priceService;
	private final SpeechFormatter formatter;

	public CurrentPriceIntentHandler( PriceService priceService, SpeechFormatter formatter ) {
		super( "GetCurrentPriceIntent",
				"Es gibt gerade ein Problem beim Abrufen des Strompreises. Bitte versuche es später noch einmal." );
		this.priceService = priceService;
		this.formatter = formatter;
	}

	@Override
	protected Optional<Response> answer( HandlerInput input, String token ) {
		PriceSchedule schedule = priceService.prices( token );
		Price current = schedule.current( );
		if ( current == null ) {
			throw new TibberApiException( "Tibber liefert keinen aktuellen Preis" );
		}
		return speak( input, "Der aktuelle Strompreis beträgt " + formatter.unitPrice( current.total( ), schedule.currency( ) )
		                     + " pro Kilowattstunde. Das Preisniveau ist " + formatter.level( current.level( ) ) + "." );
	}
}
