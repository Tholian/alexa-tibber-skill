package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;


public class FallbackIntentHandler implements RequestHandler {

	@Override
	public boolean canHandle( HandlerInput input ) {
		return input.matches( Predicates.intentName( "AMAZON.FallbackIntent" ) );
	}

	@Override
	public Optional<Response> handle( HandlerInput input ) {
		return input.getResponseBuilder( )
				.withSpeech( "Das habe ich leider nicht verstanden. Du kannst fragen: Was kostet Strom gerade? Oder: Wann ist Strom am günstigsten?" )
				.withReprompt( "Wie kann ich dir helfen?" )
				.withShouldEndSession( false )
				.build( );
	}
}
