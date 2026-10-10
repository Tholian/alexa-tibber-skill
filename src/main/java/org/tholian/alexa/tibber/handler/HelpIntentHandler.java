package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;


public class HelpIntentHandler implements RequestHandler {

	@Override
	public boolean canHandle( HandlerInput input ) {
		return input.matches( Predicates.intentName( "AMAZON.HelpIntent" ) );
	}

	@Override
	public Optional<Response> handle( HandlerInput input ) {
		String speech = "Mit dem Tibber Strompreis Skill kannst du nach Strompreisen fragen, etwa: Was kostet Strom um 18 Uhr? "
		                + "Oder: Ist Strom gerade günstig? Nach günstigen Zeiten: Wann sind die günstigsten drei Stunden? "
		                + "Nach deinem Verbrauch: Wie viel Strom habe ich letzte Woche verbraucht? Oder: Wie viel Watt verbrauche ich gerade? "
		                + "Und nach deiner Einspeisung: Wie viel habe ich im September eingespeist? "
		                + "Was möchtest du wissen?";
		return input.getResponseBuilder( )
				.withSpeech( speech )
				.withReprompt( "Was möchtest du wissen?" )
				.withShouldEndSession( false )
				.build( );
	}
}
