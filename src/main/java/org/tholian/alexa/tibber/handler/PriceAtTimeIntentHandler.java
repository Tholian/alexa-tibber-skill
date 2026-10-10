package org.tholian.alexa.tibber.handler;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;

import org.jspecify.annotations.Nullable;

import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.service.PriceSchedule;
import org.tholian.alexa.tibber.service.PriceService;
import org.tholian.alexa.tibber.service.SpeechFormatter;

/**
 * KI-generiert: „Was kostet Strom (morgen) um 18 Uhr?“ – Preis der Viertelstunde, in die die Uhrzeit fällt.
 * Ohne Datum ist der nächste solche Zeitpunkt gemeint (eine bereits vergangene Uhrzeit liegt morgen).
 */
public class PriceAtTimeIntentHandler extends TibberIntentHandler {

	static final String ONLY_TODAY_AND_TOMORROW = "Ich kenne nur die Preise für heute und morgen.";
	static final String TOMORROW_NOT_PUBLISHED =
			"Die Preise für morgen hat Tibber noch nicht veröffentlicht. Das passiert meist am frühen Nachmittag.";
	private static final String ASK_TIME = "Zu welcher Uhrzeit genau? Frag zum Beispiel: Was kostet Strom um 18 Uhr?";

	private final PriceService priceService;
	private final SpeechFormatter formatter;
	private final Clock clock;

	public PriceAtTimeIntentHandler( PriceService priceService, SpeechFormatter formatter, Clock clock ) {
		super( "GetPriceAtTimeIntent",
				"Es gibt gerade ein Problem beim Abrufen der Preisdaten. Bitte versuche es später noch einmal." );
		this.priceService = priceService;
		this.formatter = formatter;
		this.clock = clock;
	}

	@Override
	protected Optional<Response> answer( HandlerInput input, String token ) {
		LocalTime time = quarterOf( Slots.value( input, "time" ) );
		if ( time == null ) {
			return Slots.askAgain( input, ASK_TIME );
		}
		LocalDate today = formatter.today( );
		String dateValue = Slots.value( input, "date" );
		LocalDate date;
		if ( dateValue == null ) {
			// KI-generiert: Ohne Datum ist der nächste Zeitpunkt gemeint – eine schon vergangene Viertelstunde liegt morgen
			Instant todayAt = today.atTime( time ).atZone( SpeechFormatter.BERLIN ).toInstant( );
			date = todayAt.plus( PriceSchedule.QUARTER ).isAfter( clock.instant( ) ) ? today : today.plusDays( 1 );
		} else {
			date = Slots.day( dateValue, today );
			if ( date == null || !( date.equals( today ) || date.equals( today.plusDays( 1 ) ) ) ) {
				return speak( input, ONLY_TODAY_AND_TOMORROW );
			}
		}
		PriceSchedule schedule = priceService.prices( token );
		Optional<Price> price = schedule.at( date.atTime( time ).atZone( SpeechFormatter.BERLIN ).toInstant( ) );
		if ( price.isEmpty( ) ) {
			return speak( input, date.equals( today ) ? "Für diesen Zeitpunkt liegt kein Preis vor." : TOMORROW_NOT_PUBLISHED );
		}
		Price slot = price.get( );
		return speak( input, SpeechFormatter.capitalize( formatter.time( slot.startsAt( ) ) ) + " kostet Strom "
		                     + formatter.unitPrice( slot.total( ), schedule.currency( ) ) + " pro Kilowattstunde. Das Preisniveau ist "
		                     + formatter.level( slot.level( ) ) + "." );
	}

	/** KI-generiert: AMAZON.TIME („18:10“) auf die Viertelstunde abgerundet; Tageszeiten wie „EV“ (abends) → {@code null}. */
	static @Nullable LocalTime quarterOf( @Nullable String value ) {
		if ( value == null ) {
			return null;
		}
		try {
			LocalTime time = LocalTime.parse( value );
			return LocalTime.of( time.getHour( ), time.getMinute( ) / 15 * 15 );
		} catch ( DateTimeParseException e ) {
			return null;
		}
	}
}
