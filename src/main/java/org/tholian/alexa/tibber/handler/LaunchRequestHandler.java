package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;

import org.springframework.stereotype.Component;

@Component
public class LaunchRequestHandler implements RequestHandler {

	@Override
	public boolean canHandle( HandlerInput input ) {
		return input.matches( Predicates.requestType( LaunchRequest.class ) );
	}

	@Override
	public Optional<Response> handle( HandlerInput input ) {
		return input.getResponseBuilder( )
				.withSpeech( "Willkommen beim Tibber Strompreis Skill! Du kannst fragen: " +
				             "Was kostet Strom gerade? Wann ist Strom am günstigsten? " +
				             "Wann ist Strom am teuersten? Oder: Wie viel Strom verbrauche ich gerade?" )
				.withReprompt( "Wie kann ich dir helfen?" )
				.withShouldEndSession( false )
				.build( );
	}
}
