package org.tholian.alexa.tibber.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties( ignoreUnknown = true )
public record TibberResponse(Data data) {

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record Data(Viewer viewer) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record Viewer(List<Home> homes) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record Home(
			CurrentSubscription currentSubscription,
			Consumption consumption
	) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record CurrentSubscription(PriceInfo priceInfo) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record PriceInfo(
			Price current,
			List<Price> today,
			List<Price> tomorrow
	) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record Consumption(List<ConsumptionNode> nodes) {
	}
}
