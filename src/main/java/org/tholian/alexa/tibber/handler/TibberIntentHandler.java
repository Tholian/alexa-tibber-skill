package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;

import lombok.extern.slf4j.Slf4j;

import org.tholian.alexa.tibber.service.TibberAuthenticationException;

/**
 * KI-generiert: Gemeinsamer Ablauf aller Handler, die Tibber abfragen:
 * Intent prüfen → Token aus dem Request lesen (fehlt er: Aufforderung zur Verknüpfung) → Antwort erzeugen.
 * Lehnt Tibber den Token ab, folgt die Aufforderung zur Neuverknüpfung; jeder andere Fehler wird mit einer
 * handler-spezifischen, gesprochenen Fehlermeldung beantwortet.
 * <p>
 * SICHERHEIT: Der Token wird nur an {@link #answer(HandlerInput, String)} weitergereicht und nie geloggt.
 * Exception-Messages der Services enthalten den Token nie.
 */
@Slf4j
abstract class TibberIntentHandler implements RequestHandler {

	private final String intentName;
	private final String errorSpeech;

	protected TibberIntentHandler( String intentName, String errorSpeech ) {
		this.intentName = intentName;
		this.errorSpeech = errorSpeech;
	}

	@Override
	public final boolean canHandle( HandlerInput input ) {
		return input.matches( Predicates.intentName( intentName ) );
	}

	@Override
	public final Optional<Response> handle( HandlerInput input ) {
		String token = AccountLinkingResponses.accessToken( input );
		if ( token == null ) {
			return AccountLinkingResponses.linkAccount( input );
		}
		try {
			return answer( input, token );
		} catch ( TibberAuthenticationException e ) {
			log.warn( "Tibber lehnt den Access-Token ab – Neuverknüpfung angefordert: {}", e.getMessage( ) );
			return AccountLinkingResponses.relinkAccount( input );
		} catch ( RuntimeException e ) {
			log.error( "Fehler bei der Verarbeitung von {}", intentName, e );
			return speak( input, errorSpeech );
		}
	}

	/** KI-generiert: Fachliche Antwort; der Token ist hier immer vorhanden und nicht leer. */
	protected abstract Optional<Response> answer( HandlerInput input, String token );

	/** KI-generiert: Einfache gesprochene Antwort (Sitzungsende nach ASK-Standard). */
	protected static Optional<Response> speak( HandlerInput input, String speech ) {
		return input.getResponseBuilder( ).withSpeech( speech ).build( );
	}
}
