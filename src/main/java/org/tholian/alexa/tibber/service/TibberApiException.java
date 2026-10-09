package org.tholian.alexa.tibber.service;

/**
 * KI-generiert: Signalisiert einen fehlgeschlagenen Aufruf der Tibber API
 * (Netzwerkfehler, Timeout, HTTP-Fehlerstatus, GraphQL-Fehler oder unlesbare Antwort).
 * <p>
 * SICHERHEIT: Die Message darf niemals den Access-Token enthalten – sie landet im Log.
 */
public class TibberApiException extends RuntimeException {

	public TibberApiException( String message ) {
		super( message );
	}

	public TibberApiException( String message, Throwable cause ) {
		super( message, cause );
	}
}
