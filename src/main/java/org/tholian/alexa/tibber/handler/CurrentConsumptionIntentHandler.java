package org.tholian.alexa.tibber.handler;

import java.time.Duration;
import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;

import org.jspecify.annotations.Nullable;

import org.tholian.alexa.tibber.model.ConsumptionNode;
import org.tholian.alexa.tibber.model.LiveMeasurement;
import org.tholian.alexa.tibber.service.CurrentUsage;
import org.tholian.alexa.tibber.service.Deadline;
import org.tholian.alexa.tibber.service.LiveService;
import org.tholian.alexa.tibber.service.SpeechFormatter;

/**
 * KI-generiert: „Wie viel (Watt) verbrauche ich gerade?“ – mit Tibber Pulse die aktuelle Leistung und der
 * Verbrauch seit Mitternacht, sonst ehrlich die zuletzt abgerechnete Stunde.
 */
public class CurrentConsumptionIntentHandler extends TibberIntentHandler {

	private final LiveService liveService;
	private final SpeechFormatter formatter;
	private final Duration liveBudget;

	/** @param liveBudget Zeitbudget für Vorabfrage und WebSocket zusammen (Alexa-Limit 8 s) */
	public CurrentConsumptionIntentHandler( LiveService liveService, SpeechFormatter formatter, Duration liveBudget ) {
		super( "GetCurrentConsumptionIntent",
				"Es gibt gerade ein Problem beim Abrufen der Verbrauchsdaten. Bitte versuche es später noch einmal." );
		this.liveService = liveService;
		this.formatter = formatter;
		this.liveBudget = liveBudget;
	}

	@Override
	protected Optional<Response> answer( HandlerInput input, String token ) {
		return switch ( liveService.currentUsage( token, Deadline.after( liveBudget ) ) ) {
			case CurrentUsage.Live live -> speak( input, liveSentence( live.measurement( ) ) );
			case CurrentUsage.LastHour lastHour -> speak( input, lastHourSentence( lastHour.node( ) ) );
		};
	}

	private String liveSentence( LiveMeasurement measurement ) {
		double power = measurement.power( ) == null ? 0 : measurement.power( );
		StringBuilder speech = new StringBuilder( "Gerade verbrauchst du " ).append( formatter.power( power ) );
		@Nullable Double production = measurement.powerProduction( );
		if ( production != null && production > 0 ) {
			speech.append( " und speist " ).append( formatter.power( production ) ).append( " ein" );
		}
		speech.append( '.' );
		@Nullable Double consumedToday = measurement.accumulatedConsumption( );
		if ( consumedToday != null ) {
			speech.append( " Heute hast du bisher " ).append( formatter.energy( consumedToday ) );
			@Nullable Double costToday = measurement.accumulatedCost( );
			if ( costToday != null ) {
				speech.append( " für " ).append( formatter.money( costToday, measurement.currency( ) ) );
			}
			speech.append( " verbraucht." );
		}
		return speech.toString( );
	}

	private String lastHourSentence( @Nullable ConsumptionNode node ) {
		if ( node == null ) {
			return "Es sind noch keine Verbrauchsdaten verfügbar.";
		}
		String from = formatter.clockTime( node.from( ) );
		if ( node.consumption( ) == null || node.cost( ) == null ) {
			return "In der letzten abgerechneten Stunde ab " + from + " sind noch keine vollständigen Verbrauchsdaten verfügbar.";
		}
		return "In der letzten abgerechneten Stunde ab " + from + " hast du " + formatter.energy( node.consumption( ) )
		       + " verbraucht und dafür " + formatter.money( node.cost( ), node.currency( ) ) + " bezahlt.";
	}
}
