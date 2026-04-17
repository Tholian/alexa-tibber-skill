package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.SessionEndedRequest;
import com.amazon.ask.request.Predicates;

import org.springframework.stereotype.Component;

@Component
public class SessionEndedRequestHandler implements RequestHandler {

	@Override
	public boolean canHandle( HandlerInput input ) {
		return input.matches( Predicates.requestType( SessionEndedRequest.class ) );
	}

	@Override
	public Optional<Response> handle( HandlerInput input ) {
		return input.getResponseBuilder( ).build( );
	}
}
