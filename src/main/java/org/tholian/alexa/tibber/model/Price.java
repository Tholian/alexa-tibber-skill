package org.tholian.alexa.tibber.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;

/**
 * KI-generiert: Ein Tibber-Preis für ein Zeitfenster (Viertelstunde oder Stunde).
 * {@code energy} (reiner Energiepreis) und {@code tax} (Steuern und Abgaben) ergeben zusammen {@code total}.
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record Price(
		double total,
		@Nullable Double energy,
		@Nullable Double tax,
		@Nullable String currency,
		@Nullable String level,
		String startsAt
) {

	/** KI-generiert: Beginn des Zeitfensters als Instant (Tibber liefert ISO-8601 mit Offset). */
	public Instant start( ) {
		return OffsetDateTime.parse( startsAt ).toInstant( );
	}

	/** KI-generiert: Kopie mit anderem Beginn – zum Aufteilen von Stunden- in Viertelstundenwerte. */
	public Price withStart( Instant start, ZoneId zone ) {
		return new Price( total, energy, tax, currency, level, start.atZone( zone ).toOffsetDateTime( ).toString( ) );
	}
}
