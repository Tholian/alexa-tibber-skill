package org.tholian.alexa.tibber.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jspecify.annotations.Nullable;

/**
 * KI-generiert: Antwortstruktur der Tibber GraphQL API – gemeinsames Modell für alle Abfragen des Skills.
 * Jede Abfrage füllt nur die Felder, die sie anfordert; alles andere bleibt {@code null}.
 * Bei Fehlern (z. B. ungültiger Token) ist {@code data} {@code null} und {@code errors} befüllt.
 * <p>
 * Datensparsamkeit: Personenbezogene Felder (Name, Adresse, Zählpunkt) sind bewusst nicht modelliert.
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public record TibberResponse( @Nullable Data data, @Nullable List<GraphQLError> errors ) {

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record GraphQLError( @Nullable String message, @Nullable Extensions extensions ) {
	}

	/** KI-generiert: GraphQL-Fehlererweiterung; Tibber liefert hier z. B. {@code code = "UNAUTHENTICATED"}. */
	@JsonIgnoreProperties( ignoreUnknown = true )
	public record Extensions( @Nullable String code ) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record Data( @Nullable Viewer viewer ) {
	}

	/** KI-generiert: {@code websocketSubscriptionUrl} = Endpoint für Live-Daten (GraphQL-Subscriptions). */
	@JsonIgnoreProperties( ignoreUnknown = true )
	public record Viewer( @Nullable String websocketSubscriptionUrl, @Nullable List<Home> homes ) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record Home(
			@Nullable String id,
			@Nullable Features features,
			@Nullable CurrentSubscription currentSubscription,
			@Nullable Consumption consumption,
			@Nullable Production production
	) {
	}

	/** KI-generiert: {@code realTimeConsumptionEnabled} = Tibber Pulse (o. Ä.) liefert Live-Daten. */
	@JsonIgnoreProperties( ignoreUnknown = true )
	public record Features( @Nullable Boolean realTimeConsumptionEnabled ) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record CurrentSubscription( @Nullable PriceInfo priceInfo ) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record PriceInfo(
			@Nullable Price current,
			@Nullable List<Price> today,
			@Nullable List<Price> tomorrow
	) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record Consumption( @Nullable List<ConsumptionNode> nodes ) {
	}

	@JsonIgnoreProperties( ignoreUnknown = true )
	public record Production( @Nullable List<ProductionNode> nodes ) {
	}
}
