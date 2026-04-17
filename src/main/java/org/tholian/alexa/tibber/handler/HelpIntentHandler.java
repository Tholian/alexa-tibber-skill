package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;

import org.springframework.stereotype.Component;

@Component
public class HelpIntentHandler implements RequestHandler {

	@Override
	public boolean canHandle( HandlerInput input ) {
		return input.matches( Predicates.intentName( "AMAZON.HelpIntent" ) );
	}

	@Override
	public Optional<Response> handle( HandlerInput input ) {
		String speech = "Mit dem Tibber Strompreis Skill kannst du Folgendes fragen: " +
		                "Erstens: Was kostet Strom gerade? " +
		                "Zweitens: Wann ist Strom am günstigsten? " +
		                "Drittens: Wann ist Strom am teuersten? " +
		                "Und viertens: Wie viel Strom verbrauche ich gerade? " +
		                "Was möchtest du wissen?";
		return input.getResponseBuilder( )
				.withSpeech( speech )
				.withReprompt( "Was möchtest du wissen?" )
				.withShouldEndSession( false )
				.build( );
	}
}
