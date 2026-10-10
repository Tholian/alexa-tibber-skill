package org.tholian.alexa.tibber.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;

/**
 * KI-generiert: Einspeisung eines Zeitfensters (z. B. PV-Anlage). {@code production} in kWh,
 * {@code profit} = Erlös; beide {@code null}, solange Tibber das Zeitfenster nicht abgerechnet hat.
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record ProductionNode(
		String from,
		String to,
		@Nullable Double production,
		@Nullable Double profit,
		@Nullable String currency
) {
}
