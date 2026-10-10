package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;


public class LaunchRequestHandler implements RequestHandler {

	@Override
	public boolean canHandle( HandlerInput input ) {
		return input.matches( Predicates.requestType( LaunchRequest.class ) );
	}

	@Override
	public Optional<Response> handle( HandlerInput input ) {
		return input.getResponseBuilder( )
				.withSpeech( "Willkommen beim Tibber Strompreis Skill! Frag zum Beispiel: Was kostet Strom gerade? "
				             + "Wann soll ich die Waschmaschine anmachen? Oder: Wie viel Strom habe ich gestern verbraucht?" )
				.withReprompt( "Wie kann ich dir helfen?" )
				.withShouldEndSession( false )
				.build( );
	}
}
