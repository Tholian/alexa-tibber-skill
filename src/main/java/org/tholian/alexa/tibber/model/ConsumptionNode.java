package org.tholian.alexa.tibber.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties( ignoreUnknown = true )
public record ConsumptionNode(
		String from,
		String to,
		Double consumption,
		String unit,
		Double cost
) {
}
