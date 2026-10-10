package org.tholian.alexa.tibber.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.jspecify.annotations.Nullable;

/**
 * KI-generiert: Request-Body für die Tibber GraphQL API ({@code {"query": "...", "variables": {...}}}).
 * <p>
 * SICHERHEIT: Variable Werte (z. B. aus Alexa-Slots abgeleitet) gehören ausschließlich in {@code variables},
 * nie per String-Verkettung in {@code query} – so ist keine GraphQL-Injection möglich.
 * Ohne Variablen entfällt das Feld im JSON.
 */
@JsonInclude( JsonInclude.Include.NON_NULL )
public record GraphQLRequest( String query, @Nullable Map<String, Object> variables ) {

	public GraphQLRequest( String query ) {
		this( query, null );
	}
}
