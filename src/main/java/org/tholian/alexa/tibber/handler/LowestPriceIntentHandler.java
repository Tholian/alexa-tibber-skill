package org.tholian.alexa.tibber.handler;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;

import org.tholian.alexa.tibber.service.PriceSchedule;
import org.tholian.alexa.tibber.service.PriceService;
import org.tholian.alexa.tibber.service.PriceWindow;
import org.tholian.alexa.tibber.service.PriceWindows;
import org.tholian.alexa.tibber.service.SpeechFormatter;

/**
 * KI-generiert: „Wann ist Strom am günstigsten?“ – günstigstes 60-Minuten-Fenster (4 Viertelstunden)
 * ab der laufenden Viertelstunde, über alle bekannten Preise (heute und – sobald veröffentlicht – morgen).
 */
public class LowestPriceIntentHandler extends TibberIntentHandler {

	static final int ONE_HOUR = 4;

	private final PriceService priceService;
	private final SpeechFormatter formatter;
	private final Clock clock;

	public LowestPriceIntentHandler( PriceService priceService, SpeechFormatter formatter, Clock clock ) {
		super( "GetLowestPriceIntent",
				"Es gibt gerade ein Problem beim Abrufen der Preisdaten. Bitte versuche es später noch einmal." );
		this.priceService = priceService;
		this.formatter = formatter;
		this.clock = clock;
	}

	@Override
	protected Optional<Response> answer( HandlerInput input, String token ) {
		PriceSchedule schedule = priceService.prices( token );
		Instant now = clock.instant( );
		Optional<PriceWindow> window = PriceWindows.cheapest( schedule.from( now ), ONE_HOUR );
		if ( window.isEmpty( ) ) {
			return speak( input, "Es sind keine Preisdaten für die nächsten Stunden verfügbar." );
		}
		return speak( input, "Die günstigste Stunde beginnt " + formatter.startOf( window.get( ).first( ), now )
		                     + ", im Schnitt " + formatter.unitPrice( window.get( ).average( ), schedule.currency( ) )
		                     + " pro Kilowattstunde." );
	}
}
