package org.tholian.alexa.tibber.handler;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;

import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.service.PriceSchedule;
import org.tholian.alexa.tibber.service.PriceService;
import org.tholian.alexa.tibber.service.PriceWindows;
import org.tholian.alexa.tibber.service.SpeechFormatter;
import org.tholian.alexa.tibber.service.TibberApiException;

/**
 * KI-generiert: „Ist Strom gerade günstig?“ – Urteil nach dem Tibber-Preisniveau der laufenden Viertelstunde.
 * Ist der Strom nicht günstig, nennt die Antwort zusätzlich die nächste günstigste Stunde.
 */
public class IsPriceCheapIntentHandler extends TibberIntentHandler {

	private final PriceService priceService;
	private final SpeechFormatter formatter;
	private final Clock clock;

	public IsPriceCheapIntentHandler( PriceService priceService, SpeechFormatter formatter, Clock clock ) {
		super( "IsPriceCheapIntent",
				"Es gibt gerade ein Problem beim Abrufen des Strompreises. Bitte versuche es später noch einmal." );
		this.priceService = priceService;
		this.formatter = formatter;
		this.clock = clock;
	}

	@Override
	protected Optional<Response> answer( HandlerInput input, String token ) {
		PriceSchedule schedule = priceService.prices( token );
		Price current = schedule.current( );
		if ( current == null ) {
			throw new TibberApiException( "Tibber liefert keinen aktuellen Preis" );
		}
		String currency = schedule.currency( );
		String price = formatter.unitPrice( current.total( ), currency );
		String level = current.level( ) == null ? "" : current.level( );
		String verdict = switch ( level ) {
			case "VERY_CHEAP", "CHEAP" -> "Ja, Strom ist gerade " + formatter.level( level ) + ": " + price + " pro Kilowattstunde.";
			case "NORMAL" -> "Strom kostet gerade " + price + ", das ist ein normales Preisniveau.";
			case "EXPENSIVE", "VERY_EXPENSIVE" -> "Nein, Strom ist gerade " + formatter.level( level ) + ": " + price + ".";
			default -> "Strom kostet gerade " + price + " pro Kilowattstunde.";
		};
		if ( level.equals( "VERY_CHEAP" ) || level.equals( "CHEAP" ) ) {
			return speak( input, verdict );
		}
		Instant now = clock.instant( );
		String hint = PriceWindows.cheapest( schedule.from( now ), LowestPriceIntentHandler.ONE_HOUR )
				.map( window -> " Die günstigste Stunde beginnt " + formatter.startOf( window.first( ), now )
				                + " mit im Schnitt " + formatter.unitPrice( window.average( ), currency ) + "." )
				.orElse( "" );
		return speak( input, verdict + hint );
	}
}
