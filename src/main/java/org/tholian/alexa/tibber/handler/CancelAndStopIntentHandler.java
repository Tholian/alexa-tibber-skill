package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;


public class CancelAndStopIntentHandler implements RequestHandler {

	@Override
	public boolean canHandle( HandlerInput input ) {
		// KI-generiert: NavigateHomeIntent (Interaction Model) beendet die Session wie Stop/Cancel
		return input.matches( Predicates.intentName( "AMAZON.CancelIntent" )
				.or( Predicates.intentName( "AMAZON.StopIntent" ) )
				.or( Predicates.intentName( "AMAZON.NavigateHomeIntent" ) ) );
	}

	@Override
	public Optional<Response> handle( HandlerInput input ) {
		return input.getResponseBuilder( )
				.withSpeech( "Auf Wiedersehen!" )
				.withShouldEndSession( true )
				.build( );
	}
}
