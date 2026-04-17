package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;

import org.springframework.stereotype.Component;

@Component
public class CancelAndStopIntentHandler implements RequestHandler {

	@Override
	public boolean canHandle( HandlerInput input ) {
		return input.matches( Predicates.intentName( "AMAZON.CancelIntent" )
				.or( Predicates.intentName( "AMAZON.StopIntent" ) ) );
	}

	@Override
	public Optional<Response> handle( HandlerInput input ) {
		return input.getResponseBuilder( )
				.withSpeech( "Auf Wiedersehen!" )
				.withShouldEndSession( true )
				.build( );
	}
}
