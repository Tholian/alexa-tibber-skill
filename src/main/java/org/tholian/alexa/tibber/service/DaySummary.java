package org.tholian.alexa.tibber.service;

import java.util.List;
import java.util.Optional;

import org.tholian.alexa.tibber.model.Price;

/**
 * KI-generiert: Tagesüberblick – günstigste und teuerste Viertelstunde (bei Gleichstand die früheste)
 * und arithmetischer Durchschnitt.
 */
public record DaySummary( Price min, Price max, double average ) {

	public static Optional<DaySummary> of( List<Price> slots ) {
		if ( slots.isEmpty( ) ) {
			return Optional.empty( );
		}
		Price min = slots.getFirst( );
		Price max = slots.getFirst( );
		double sum = 0;
		for ( Price price : slots ) {
			if ( price.total( ) < min.total( ) ) {
				min = price;
			}
			if ( price.total( ) > max.total( ) ) {
				max = price;
			}
			sum += price.total( );
		}
		return Optional.of( new DaySummary( min, max, sum / slots.size( ) ) );
	}
}
