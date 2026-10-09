package org.tholian.alexa.tibber.service;

/**
 * KI-generiert: Tibber hat den Access-Token abgelehnt (HTTP 401/403 oder GraphQL-Fehlercode
 * {@code UNAUTHENTICATED}) – typischerweise, weil der Nutzer den Token bei Tibber widerrufen hat.
 * Der Skill fordert dann zur Neuverknüpfung auf.
 * <p>
 * SICHERHEIT: Die Message enthält niemals den Token.
 */
public class TibberAuthenticationException extends TibberApiException {

	public TibberAuthenticationException( String message ) {
		super( message );
	}
}
