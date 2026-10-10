package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;

import org.jspecify.annotations.Nullable;

import org.tholian.alexa.tibber.service.DatePeriod;
import org.tholian.alexa.tibber.service.EnergyKind;
import org.tholian.alexa.tibber.service.EnergyResult;
import org.tholian.alexa.tibber.service.EnergyService;
import org.tholian.alexa.tibber.service.SpeechFormatter;

/**
 * KI-generiert: „Wie viel Strom habe ich gestern verbraucht?“ / „Was hat mich Strom im September gekostet?“ –
 * Verbrauch, Kosten und verbrauchsgewichteter Durchschnittspreis für einen Zeitraum.
 */
public class ConsumptionForPeriodIntentHandler extends TibberIntentHandler {

	static final String UNSUPPORTED_PERIOD =
			"Diesen Zeitraum kann ich nicht auswerten. Frag zum Beispiel: Wie viel Strom habe ich gestern verbraucht?";

	private final EnergyService energyService;
	private final SpeechFormatter formatter;

	public ConsumptionForPeriodIntentHandler( EnergyService energyService, SpeechFormatter formatter ) {
		super( "GetConsumptionForPeriodIntent",
				"Es gibt gerade ein Problem beim Abrufen der Verbrauchsdaten. Bitte versuche es später noch einmal." );
		this.energyService = energyService;
		this.formatter = formatter;
	}

	@Override
	protected Optional<Response> answer( HandlerInput input, String token ) {
		Optional<DatePeriod> period = DatePeriod.parse( Slots.value( input, "date" ) );
		if ( period.isEmpty( ) ) {
			return Slots.askAgain( input, UNSUPPORTED_PERIOD );
		}
		return switch ( energyService.total( token, period.get( ), EnergyKind.CONSUMPTION ) ) {
			case EnergyResult.Future _ -> speak( input, "Dieser Zeitraum liegt in der Zukunft." );
			case EnergyResult.TooOld _ -> Slots.askAgain( input, UNSUPPORTED_PERIOD );
			case EnergyResult.NoData _ -> speak( input, "Für diesen Zeitraum liegen keine Verbrauchsdaten vor." );
			case EnergyResult.Total total -> speak( input, sentence( period.get( ), total ) );
		};
	}

	private String sentence( DatePeriod period, EnergyResult.Total total ) {
		StringBuilder speech = new StringBuilder( SpeechFormatter.capitalize( formatter.period( period ) ) )
				.append( " hast du " ).append( formatter.coverage( total.coverage( ) ) )
				.append( formatter.energy( total.energyKwh( ) ) ).append( " verbraucht" );
		@Nullable Double cost = total.amount( );
		if ( cost != null ) {
			speech.append( " und dafür " ).append( formatter.money( cost, total.currency( ) ) ).append( " bezahlt" );
			if ( total.energyKwh( ) > 0 ) {
				speech.append( ", im Schnitt " ).append( formatter.unitPrice( cost / total.energyKwh( ), total.currency( ) ) )
						.append( " pro Kilowattstunde" );
			}
		}
		return speech.append( '.' ).toString( );
	}
}
