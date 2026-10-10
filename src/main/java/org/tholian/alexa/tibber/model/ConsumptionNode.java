package org.tholian.alexa.tibber.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;

/**
 * KI-generiert: Verbrauch eines Zeitfensters. {@code consumption} und {@code cost} sind {@code null},
 * solange Tibber das Zeitfenster noch nicht abgerechnet hat.
 * Hinweis: Ein Feld {@code unit} existiert im Tibber-Schema nicht (GRAPHQL_VALIDATION_FAILED).
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record ConsumptionNode(
		String from,
		String to,
		@Nullable Double consumption,
		@Nullable Double cost,
		@Nullable String currency
) {
}
