package org.tholian.alexa.tibber.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;

/**
 * KI-generiert: Verbrauch eines Zeitfensters. {@code consumption} und {@code cost} sind {@code null},
 * solange Tibber die Stunde noch nicht abgerechnet hat.
 * Hinweis: Das frühere Feld {@code unit} wurde entfernt – es existiert im Tibber-Schema nicht
 * (GRAPHQL_VALIDATION_FAILED) und wurde nie befüllt.
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record ConsumptionNode(
		String from,
		String to,
		@Nullable Double consumption,
		@Nullable Double cost
) {
}
