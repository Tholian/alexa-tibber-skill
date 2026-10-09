package org.tholian.alexa.tibber.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;

/**
 * KI-generiert: Ein Tibber-Preis für ein Zeitfenster.
 * {@code currency} wird nur für den aktuellen Preis abgefragt und ist bei today/tomorrow {@code null}.
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record Price(
		double total,
		@Nullable String currency,
		@Nullable String level,
		String startsAt
) {
}
