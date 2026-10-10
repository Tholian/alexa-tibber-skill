package org.tholian.alexa.tibber.handler;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;

import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.service.DaySummary;
import org.tholian.alexa.tibber.service.PriceSchedule;
import org.tholian.alexa.tibber.service.PriceService;
import org.tholian.alexa.tibber.service.SpeechFormatter;

/**
 * KI-generiert: „Wie teuer wird Strom morgen?“ – Spanne, Durchschnitt und günstigste/teuerste Viertelstunde.
 * Heute (auch ohne Datum): nur die verbleibenden Viertelstunden ab der laufenden; morgen: ganzer Tag.
 */
public class DayPriceSummaryIntentHandler extends TibberIntentHandler {

	private final PriceService priceService;
	private final SpeechFormatter formatter;
	private final Clock clock;

	public DayPriceSummaryIntentHandler( PriceService priceService, SpeechFormatter formatter, Clock clock ) {
		super( "GetDayPriceSummaryIntent",
				"Es gibt gerade ein Problem beim Abrufen der Preisdaten. Bitte versuche es später noch einmal." );
		this.priceService = priceService;
		this.formatter = formatter;
		this.clock = clock;
	}

	@Override
	protected Optional<Response> answer( HandlerInput input, String token ) {
		LocalDate today = formatter.today( );
		String dateValue = Slots.value( input, "date" );
		LocalDate date = dateValue == null ? today : Slots.day( dateValue, today );
		if ( date == null || !( date.equals( today ) || date.equals( today.plusDays( 1 ) ) ) ) {
			return speak( input, PriceAtTimeIntentHandler.ONLY_TODAY_AND_TOMORROW );
		}
		PriceSchedule schedule = priceService.prices( token );
		boolean isToday = date.equals( today );
		Instant now = clock.instant( );
		List<Price> slots = schedule.day( date ).stream( )
				.filter( p -> !isToday || p.start( ).plus( PriceSchedule.QUARTER ).isAfter( now ) )
				.toList( );
		Optional<DaySummary> summary = DaySummary.of( slots );
		if ( summary.isEmpty( ) ) {
			return speak( input, isToday ? "Für heute liegen keine weiteren Preise vor." : PriceAtTimeIntentHandler.TOMORROW_NOT_PUBLISHED );
		}
		DaySummary day = summary.get( );
		String currency = schedule.currency( );
		return speak( input, ( isToday ? "Für den Rest des Tages kostet Strom" : "Morgen kostet Strom" )
		                     + " zwischen " + formatter.unitPrice( day.min( ).total( ), currency )
		                     + " und " + formatter.unitPrice( day.max( ).total( ), currency )
		                     + ", im Schnitt " + formatter.unitPrice( day.average( ), currency ) + " pro Kilowattstunde."
		                     + " Am günstigsten ist es um " + formatter.clockTime( day.min( ).startsAt( ) )
		                     + ", am teuersten um " + formatter.clockTime( day.max( ).startsAt( ) ) + "." );
	}
}
