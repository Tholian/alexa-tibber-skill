package org.tholian.alexa.tibber.service;

import java.time.Instant;
import java.util.List;

import org.tholian.alexa.tibber.model.Price;

/**
 * KI-generiert: Zusammenhängendes Zeitfenster aus Viertelstunden mit arithmetischem Durchschnittspreis.
 */
public record PriceWindow( List<Price> slots, double average ) {

	public PriceWindow {
		slots = List.copyOf( slots );
	}

	public Price first( ) {
		return slots.getFirst( );
	}

	/** KI-generiert: Ende = Beginn der letzten Viertelstunde + 15 Minuten. */
	public Instant end( ) {
		return slots.getLast( ).start( ).plus( PriceSchedule.QUARTER );
	}
}
