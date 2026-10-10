package org.tholian.alexa.tibber.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;

/**
 * KI-generiert: Eine Live-Messung des Tibber Pulse. {@code power}/{@code powerProduction} in Watt,
 * {@code accumulatedConsumption} (kWh) und {@code accumulatedCost} seit Mitternacht.
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record LiveMeasurement(
		@Nullable String timestamp,
		@Nullable Double power,
		@Nullable Double powerProduction,
		@Nullable Double accumulatedConsumption,
		@Nullable Double accumulatedCost,
		@Nullable String currency
) {
}
