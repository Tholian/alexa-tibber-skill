package org.tholian.alexa.tibber.service;

import org.jspecify.annotations.Nullable;

import org.tholian.alexa.tibber.model.ConsumptionNode;
import org.tholian.alexa.tibber.model.LiveMeasurement;

/**
 * KI-generiert: Aktueller Verbrauch – entweder eine Live-Messung (Tibber Pulse) oder, als Rückfall,
 * die letzte abgerechnete Stunde ({@code node} fehlt, wenn Tibber noch keine Werte hat).
 */
public sealed interface CurrentUsage {

	record Live( LiveMeasurement measurement ) implements CurrentUsage {
	}

	record LastHour( @Nullable ConsumptionNode node ) implements CurrentUsage {
	}
}
