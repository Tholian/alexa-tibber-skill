package org.tholian.alexa.tibber.linking;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * KI-generiert: HTTP-Antwort im Format von API Gateway HTTP API (Payload 2.0).
 * SICHERHEIT: Jede Antwort trägt dieselben Sicherheits-Header (CSP, kein Framing, kein Caching, Referrer nur same-origin, HSTS).
 * Der Location-Header kann den Token im Fragment enthalten und wird deshalb in {@link #toString()} geschwärzt.
 */
record HttpResult( int statusCode, Map<String, String> headers, String body ) {

	static final Map<String, String> SECURITY_HEADERS = Map.of(
			"Content-Security-Policy", LinkingPage.CSP,
			"X-Frame-Options", "DENY",
			"X-Content-Type-Options", "nosniff",
			// SICHERHEIT: "same-origin" statt "no-referrer": Bei no-referrer senden Browser beim Formular-POST "Origin: null",
			// wodurch die Origin-Prüfung jeden echten Submit ablehnen würde. Same-origin-POSTs tragen so den echten Origin;
			// Cross-Origin-Navigationen (Weiterleitung zu Amazon, Datenschutz-Link mit rel="noreferrer") senden weiterhin keinen Referrer.
			"Referrer-Policy", "same-origin",
			"Cache-Control", "no-store",
			"Pragma", "no-cache",
			"Strict-Transport-Security", "max-age=31536000" );

	static HttpResult html( int statusCode, String html ) {
		Map<String, String> headers = new LinkedHashMap<>( SECURITY_HEADERS );
		headers.put( "Content-Type", "text/html; charset=utf-8" );
		return new HttpResult( statusCode, headers, html );
	}

	static HttpResult error( int statusCode, String message ) {
		return html( statusCode, LinkingPage.error( message ) );
	}

	/** KI-generiert: 302 an eine bereits gegen die Allowlist geprüfte Amazon-URL. */
	static HttpResult redirect( String location ) {
		Map<String, String> headers = new LinkedHashMap<>( SECURITY_HEADERS );
		headers.put( "Location", location );
		return new HttpResult( 302, headers, "" );
	}

	/** KI-generiert: SICHERHEIT: Location (Token im Fragment) wird nie ausgegeben. */
	@Override
	public String toString( ) {
		Map<String, String> redacted = new LinkedHashMap<>( headers );
		redacted.computeIfPresent( "Location", ( name, value ) -> "<redacted>" );
		return "HttpResult[statusCode=" + statusCode + ", headers=" + redacted + ", bodyLength=" + body.length( ) + "]";
	}

	ObjectNode toJson( ObjectMapper mapper ) {
		ObjectNode node = mapper.createObjectNode( );
		node.put( "statusCode", statusCode );
		ObjectNode headerNode = node.putObject( "headers" );
		headers.forEach( headerNode::put );
		node.put( "body", body );
		node.put( "isBase64Encoded", false );
		return node;
	}
}
