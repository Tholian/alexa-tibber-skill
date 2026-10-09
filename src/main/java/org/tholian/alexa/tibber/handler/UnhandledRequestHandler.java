package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Request;
import com.amazon.ask.model.Response;

import lombok.extern.slf4j.Slf4j;

/**
 * KI-generiert: Auffang-Handler für Requests ohne passenden spezifischen Handler.
 * Das ASK SDK 2.86.0 liefert in diesem Fall {@code null} statt eine Exception zu werfen; Exception-Handler
 * sehen unbekannte Intents daher nie, und Alexa meldet nur einen generischen Skill-Fehler.
 * <p>
 * Muss als LETZTER Handler registriert werden, da der erste passende Handler gewinnt.
 * <p>
 * SICHERHEIT: Geloggt wird nur der Intent-Name bzw. der Request-Typ, nie der Envelope (enthält den accessToken).
 */
@Slf4j
public class UnhandledRequestHandler implements RequestHandler {

	@Override
	public boolean canHandle( HandlerInput input ) {
		return true;
	}

	@Override
	public Optional<Response> handle( HandlerInput input ) {
		Request request = input.getRequestEnvelope( ).getRequest( );
		String name = request instanceof IntentRequest ir
				? ir.getIntent( ).getName( )
				: request.getClass( ).getSimpleName( );
		log.warn( "Kein passender Handler für Request: {}", name );
		return input.getResponseBuilder( )
				.withSpeech( GenericExceptionHandler.SPEECH )
				.withReprompt( "Wie kann ich dir helfen?" )
				.withShouldEndSession( false )
				.build( );
	}
}
