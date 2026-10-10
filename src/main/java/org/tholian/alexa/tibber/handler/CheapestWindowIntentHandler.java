package org.tholian.alexa.tibber.handler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.SlotConfirmationStatus;

import org.jspecify.annotations.Nullable;

import org.tholian.alexa.tibber.service.PriceSchedule;
import org.tholian.alexa.tibber.service.PriceService;
import org.tholian.alexa.tibber.service.PriceWindow;
import org.tholian.alexa.tibber.service.PriceWindows;
import org.tholian.alexa.tibber.service.SpeechFormatter;

/**
 * KI-generiert: „Wann soll ich die Waschmaschine anmachen?“ / „Wann sind die günstigsten 3 Stunden?“ –
 * günstigstes zusammenhängendes Zeitfenster der gewünschten Dauer ab der laufenden Viertelstunde.
 * Fehlt die Dauer, fragt Alexa über das Dialog-Modell nach (Auto-Delegation); dieser Handler delegiert
 * nur defensiv, falls der Intent trotzdem ohne Dauer ankommt.
 */
public class CheapestWindowIntentHandler extends TibberIntentHandler {

	/**
	 * KI-generiert: Obergrenze 7 Tage – längere Dauern sind sinnlos und würden nur Rechenzeit kosten.
	 * SICHERHEIT: Begrenzt die Rechenzeit für Nutzereingaben aus einem Slot.
	 */
	static final int MAX_QUARTERS = 7 * 24 * 4;
	private static final long SECONDS_PER_QUARTER = PriceSchedule.QUARTER.toSeconds( );
	private static final String ASK_DURATION = "Wie lange soll das Gerät laufen? Sag zum Beispiel: drei Stunden.";

	private final PriceService priceService;
	private final SpeechFormatter formatter;
	private final Clock clock;

	public CheapestWindowIntentHandler( PriceService priceService, SpeechFormatter formatter, Clock clock ) {
		super( "GetCheapestWindowIntent",
				"Es gibt gerade ein Problem beim Abrufen der Preisdaten. Bitte versuche es später noch einmal." );
		this.priceService = priceService;
		this.formatter = formatter;
		this.clock = clock;
	}

	@Override
	protected Optional<Response> answer( HandlerInput input, String token ) {
		String rawDuration = Slots.value( input, "duration" );
		if ( rawDuration == null ) {
			// KI-generiert: Alexa soll die Dauer selbst erfragen (Dialog-Modell mit Elicitation-Prompt)
			IntentRequest request = (IntentRequest) input.getRequestEnvelope( ).getRequest( );
			return input.getResponseBuilder( ).addDelegateDirective( request.getIntent( ) ).build( );
		}
		Integer quarters = quartersOf( rawDuration );
		if ( quarters == null ) {
			// KI-generiert: ElicitSlot statt askAgain – so ordnet Alexa die nächste Antwort („drei Stunden“) wieder dem
			// Slot duration zu, statt sie als unbekannte Äußerung im Fallback zu behandeln.
			IntentRequest request = (IntentRequest) input.getRequestEnvelope( ).getRequest( );
			Intent original = request.getIntent( );
			Map<String, Slot> slots = new HashMap<>( original.getSlots( ) == null ? Map.of( ) : original.getSlots( ) );
			slots.put( "duration", Slot.builder( ).withName( "duration" ).withConfirmationStatus( SlotConfirmationStatus.NONE ).build( ) );
			Intent updated = Intent.builder( ).withName( original.getName( ) ).withSlots( slots )
					.withConfirmationStatus( original.getConfirmationStatus( ) ).build( );
			return input.getResponseBuilder( ).addElicitSlotDirective( "duration", updated )
					.withSpeech( ASK_DURATION ).withReprompt( ASK_DURATION ).build( );
		}
		PriceSchedule schedule = priceService.prices( token );
		Instant now = clock.instant( );
		String length = formatter.duration( quarters * 15 );
		Optional<PriceWindow> window = PriceWindows.cheapest( schedule.from( now ), quarters );
		if ( window.isEmpty( ) ) {
			return speak( input, "Für " + length + " reichen die bekannten Preise nicht."
			                     + schedule.lastSlot( )
					                     .map( last -> " Tibber hat Preise bis " + formatter.time( last.startsAt( ) ) + " veröffentlicht." )
					                     .orElse( "" ) );
		}
		return speak( input, "Der günstigste Zeitraum für " + length + " beginnt " + formatter.startOf( window.get( ).first( ), now )
		                     + ", im Schnitt " + formatter.unitPrice( window.get( ).average( ), schedule.currency( ) )
		                     + " pro Kilowattstunde." );
	}

	/** KI-generiert: ISO-8601-Dauer aus AMAZON.DURATION („PT3H“) in Viertelstunden, aufgerundet; ungültig → {@code null}. */
	static @Nullable Integer quartersOf( String isoDuration ) {
		Duration duration;
		try {
			duration = Duration.parse( isoDuration );
		} catch ( DateTimeParseException e ) {
			return null;
		}
		if ( duration.isZero( ) || duration.isNegative( ) ) {
			return null;
		}
		// KI-generiert: Vor der Arithmetik klemmen (kein Long-Überlauf); jede positive Dauer, auch unter einer Sekunde,
		// wird auf mindestens eine Viertelstunde aufgerundet (PT0.5S → 1).
		if ( duration.compareTo( Duration.ofMinutes( 15L * MAX_QUARTERS ) ) >= 0 ) {
			return MAX_QUARTERS;
		}
		if ( duration.toSeconds( ) == 0 ) {
			return 1;
		}
		long quarters = ( duration.toSeconds( ) + SECONDS_PER_QUARTER - 1 ) / SECONDS_PER_QUARTER;
		return (int) Math.min( quarters, MAX_QUARTERS );
	}
}
