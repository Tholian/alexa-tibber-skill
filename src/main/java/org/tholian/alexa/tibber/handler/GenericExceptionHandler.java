package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.exception.ExceptionHandler;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;

import lombok.extern.slf4j.Slf4j;

/**
 * KI-generiert: Letzte Verteidigungslinie – übersetzt jede unbehandelte (geworfene) Exception in eine
 * gesprochene Antwort. Unbekannte Intents ohne passenden Handler löst dieser Handler nicht aus: das ASK SDK
 * 2.86.0 liefert dann {@code null} statt einer Exception – sie beantwortet der {@link UnhandledRequestHandler}.
 * Ohne diesen Handler würde die Lambda-Funktion mit einem Fehler enden und Alexa nur „Beim angeforderten Skill ist ein Problem aufgetreten“ sagen.
 * <p>
 * SICHERHEIT: Es wird nur die Exception geloggt, nie der Request-Envelope (enthält den accessToken).
 */
@Slf4j
public class GenericExceptionHandler implements ExceptionHandler {

	static final String SPEECH =
			"Entschuldigung, das hat nicht geklappt. Du kannst zum Beispiel fragen: Was kostet Strom gerade?";

	@Override
	public boolean canHandle( HandlerInput input, Throwable throwable ) {
		return true;
	}

	@Override
	public Optional<Response> handle( HandlerInput input, Throwable throwable ) {
		log.error( "Unbehandelter Fehler bei der Request-Verarbeitung", throwable );
		return input.getResponseBuilder( )
				.withSpeech( SPEECH )
				.withReprompt( "Wie kann ich dir helfen?" )
				.withShouldEndSession( false )
				.build( );
	}
}
