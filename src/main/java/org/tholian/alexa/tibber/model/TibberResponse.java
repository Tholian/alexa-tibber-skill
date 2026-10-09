package org.tholian.alexa.tibber.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;

/**
 * KI-generiert: Antwortstruktur der Tibber GraphQL API.
 * Bei Fehlern (z. B. ungültiger Token) ist {@code data} {@code null} und {@code errors} befüllt.
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record TibberResponse( @Nullable Data data, @Nullable List<GraphQLError> errors ) {

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record GraphQLError( @Nullable String message ) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record Data( Viewer viewer ) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record Viewer( List<Home> homes ) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record Home(
			@Nullable CurrentSubscription currentSubscription,
			@Nullable Consumption consumption
	) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record CurrentSubscription( PriceInfo priceInfo ) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record PriceInfo(
			@Nullable Price current,
			List<Price> today,
			@Nullable List<Price> tomorrow
	) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record Consumption( @Nullable List<ConsumptionNode> nodes ) {
	}
}
