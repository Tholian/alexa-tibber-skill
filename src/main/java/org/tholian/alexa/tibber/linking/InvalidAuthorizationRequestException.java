package org.tholian.alexa.tibber.linking;

/**
 * KI-generiert: Ungültige Account-Linking-Anfrage. Die Message wird dem Nutzer angezeigt und darf daher
 * keine Parameterwerte enthalten (SICHERHEIT: kein Reflektieren von Eingaben, kein Token).
 */
public class InvalidAuthorizationRequestException extends RuntimeException {

	private final int status;

	public InvalidAuthorizationRequestException( String message ) {
		this( 400, message );
	}

	public InvalidAuthorizationRequestException( int status, String message ) {
		super( message );
		this.status = status;
	}

	/** KI-generiert: HTTP-Status der Fehlerseite. */
	public int status( ) {
		return status;
	}
}
