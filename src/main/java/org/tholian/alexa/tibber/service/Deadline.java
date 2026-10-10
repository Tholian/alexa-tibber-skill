package org.tholian.alexa.tibber.service;

import java.time.Duration;

/**
 * KI-generiert: Zeitbudget eines Requests, monoton über {@link System#nanoTime()} (unabhängig von der Uhrzeit).
 * Alexa bricht nach 8 s ab – aufeinanderfolgende Aufrufe (HTTP-Vorabfrage + WebSocket) teilen sich ein Budget.
 */
public final class Deadline {

	private final long expiresAtNanos;

	private Deadline( long expiresAtNanos ) {
		this.expiresAtNanos = expiresAtNanos;
	}

	public static Deadline after( Duration budget ) {
		return new Deadline( System.nanoTime( ) + budget.toNanos( ) );
	}

	/** KI-generiert: Verbleibende Zeit, nie negativ. */
	public Duration remaining( ) {
		long left = expiresAtNanos - System.nanoTime( );
		return left <= 0 ? Duration.ZERO : Duration.ofNanos( left );
	}

	/** KI-generiert: Verbleibende Zeit, höchstens {@code limit}. */
	public Duration remainingAtMost( Duration limit ) {
		Duration remaining = remaining( );
		return remaining.compareTo( limit ) < 0 ? remaining : limit;
	}
}
