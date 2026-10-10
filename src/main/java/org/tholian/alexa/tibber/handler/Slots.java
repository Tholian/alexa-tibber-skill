package org.tholian.alexa.tibber.handler;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;

import org.jspecify.annotations.Nullable;

/**
 * KI-generiert: Zugriff auf Alexa-Slots und Rückfragen bei unbrauchbaren Werten.
 * <p>
 * SICHERHEIT: Slot-Werte sind Nutzereingaben. Sie werden nur geparst (Datum, Uhrzeit, Dauer) und
 * nie ungeprüft weitergegeben – an Tibber gehen ausschließlich daraus abgeleitete, typisierte Werte als GraphQL-Variablen.
 */
final class Slots {

	/** KI-generiert: AMAZON.DATE-Wert für „jetzt“/„gerade“. */
	private static final String PRESENT_REF = "PRESENT_REF";

	private Slots( ) {
	}

	/** KI-generiert: Wert eines Slots, getrimmt; fehlend oder leer → {@code null}. */
	static @Nullable String value( HandlerInput input, String name ) {
		if ( !( input.getRequestEnvelope( ).getRequest( ) instanceof IntentRequest request ) ) {
			return null;
		}
		Map<String, Slot> slots = request.getIntent( ).getSlots( );
		if ( slots == null ) {
			return null;
		}
		Slot slot = slots.get( name );
		if ( slot == null || slot.getValue( ) == null || slot.getValue( ).isBlank( ) ) {
			return null;
		}
		return slot.getValue( ).strip( );
	}

	/**
	 * KI-generiert: Konkretes Datum aus AMAZON.DATE („2026-10-11“); {@code PRESENT_REF} („jetzt“, „gerade“) = heute;
	 * Wochen, Monate usw. → {@code null}.
	 */
	static @Nullable LocalDate day( @Nullable String value, LocalDate today ) {
		if ( value == null ) {
			return null;
		}
		if ( PRESENT_REF.equals( value ) ) {
			return today;
		}
		try {
			return LocalDate.parse( value );
		} catch ( DateTimeParseException e ) {
			return null;
		}
	}

	/** KI-generiert: Rückfrage – Sitzung bleibt offen, die Frage dient zugleich als Reprompt. */
	static Optional<Response> askAgain( HandlerInput input, String question ) {
		return input.getResponseBuilder( )
				.withSpeech( question )
				.withReprompt( question )
				.withShouldEndSession( false )
				.build( );
	}
}
