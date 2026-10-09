package org.tholian.alexa.tibber.linking;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * KI-generiert: Strenger Parser für {@code application/x-www-form-urlencoded}.
 * SICHERHEIT: Doppelte Feldnamen werden abgelehnt (keine Mehrdeutigkeit, z. B. bei redirect_uri).
 */
final class FormData {

	private static final String INVALID = "Ungültiger Parameter.";

	private FormData( ) {
	}

	static Map<String, String> parse( String body ) {
		Map<String, String> result = new HashMap<>( );
		if ( body.isEmpty( ) ) {
			return result;
		}
		for ( String pair : body.split( "&" ) ) {
			if ( pair.isEmpty( ) ) {
				continue;
			}
			int separator = pair.indexOf( '=' );
			String name = decode( separator < 0 ? pair : pair.substring( 0, separator ) );
			String value = separator < 0 ? "" : decode( pair.substring( separator + 1 ) );
			if ( result.putIfAbsent( name, value ) != null ) {
				throw new InvalidAuthorizationRequestException( INVALID );
			}
		}
		return result;
	}

	private static String decode( String value ) {
		try {
			return URLDecoder.decode( value, StandardCharsets.UTF_8 );
		} catch ( IllegalArgumentException e ) {
			throw new InvalidAuthorizationRequestException( INVALID );
		}
	}
}
