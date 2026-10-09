package org.tholian.alexa.tibber.handler;

import java.util.Optional;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;

import org.jspecify.annotations.Nullable;

/**
 * KI-generiert: Gemeinsame Bausteine rund um die Konto-Verknüpfung (Account Linking),
 * damit alle Tibber-Handler gleich reagieren.
 */
final class AccountLinkingResponses {

	static final String LINK_SPEECH = "Bitte verknüpfe zunächst deinen Tibber Account in der Alexa App.";
	static final String RELINK_SPEECH =
			"Deine Verknüpfung mit Tibber ist nicht mehr gültig. Bitte verknüpfe dein Tibber-Konto in der Alexa App neu.";

	private AccountLinkingResponses( ) {
	}

	/**
	 * KI-generiert: Liefert den von Alexa mitgeschickten Tibber-Token oder {@code null}, wenn keiner verknüpft ist.
	 * Leere Tokens zählen als nicht verknüpft. SICHERHEIT: Der Token wird nie geloggt.
	 */
	static @Nullable String accessToken( HandlerInput input ) {
		String token = input.getRequestEnvelope( ).getContext( ).getSystem( ).getUser( ).getAccessToken( );
		return ( token == null || token.isBlank( ) ) ? null : token;
	}

	/** KI-generiert: Antwort, wenn noch kein Konto verknüpft ist – inklusive LinkAccount-Card in der Alexa-App. */
	static Optional<Response> linkAccount( HandlerInput input ) {
		return input.getResponseBuilder( )
				.withSpeech( LINK_SPEECH )
				.withLinkAccountCard( )
				.build( );
	}

	/**
	 * KI-generiert: Antwort, wenn Tibber den gespeicherten Token ablehnt (z. B. bei Tibber widerrufen).
	 * Amazon behält den Token, bis der Nutzer neu verknüpft – daher aktiv zur Neuverknüpfung auffordern.
	 */
	static Optional<Response> relinkAccount( HandlerInput input ) {
		return input.getResponseBuilder( )
				.withSpeech( RELINK_SPEECH )
				.withLinkAccountCard( )
				.withShouldEndSession( true )
				.build( );
	}
}
