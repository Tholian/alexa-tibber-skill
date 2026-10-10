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
 * KI-generiert: „Wie setzt sich der Strompreis zusammen?“ – Energiepreis und Steuern/Abgaben (Tibber-Felder
 * {@code energy} und {@code tax}). Ob Netzentgelte in {@code tax} enthalten sind, dokumentiert Tibber nicht
 * (siehe BACKLOG) – die Ansage bleibt deshalb bei „Steuern und Abgaben“.
 */
public class PriceBreakdownIntentHandler extends TibberIntentHandler {

	private final PriceService priceService;
	private final SpeechFormatter formatter;

	public PriceBreakdownIntentHandler( PriceService priceService, SpeechFormatter formatter ) {
		super( "GetPriceBreakdownIntent",
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
		if ( current.energy( ) == null || current.tax( ) == null ) {
			return speak( input, "Tibber liefert für den aktuellen Preis keine Aufschlüsselung." );
		}
		String currency = schedule.currency( );
		return speak( input, "Von " + formatter.unitPrice( current.total( ), currency ) + " pro Kilowattstunde sind "
		                     + formatter.unitPrice( current.energy( ), currency ) + " Energiepreis und "
		                     + formatter.unitPrice( current.tax( ), currency ) + " Steuern und Abgaben." );
	}
}
