package org.tholian.alexa.tibber.service;

import java.util.List;
import java.util.Optional;

import org.tholian.alexa.tibber.model.Price;

/**
 * KI-generiert: Sucht das günstigste bzw. teuerste zusammenhängende Fenster aus N Viertelstunden.
 * Laufzeit O(Anzahl Viertelstunden) über Präfixsummen. Zusammenhängend heißt: jede Viertelstunde beginnt
 * genau 15 Minuten nach der vorigen (Vergleich über Instants – robust gegen Zeitumstellung und Datenlücken).
 * Bei Gleichstand gewinnt das früheste Fenster.
 */
public final class PriceWindows {

	// KI-generiert: Toleranz gegen Rundungsrauschen der Präfixsummen, damit Gleichstände stabil das früheste Fenster liefern
	private static final double EPSILON = 1e-9;

	private PriceWindows( ) {
	}

	public static Optional<PriceWindow> cheapest( List<Price> slots, int quarters ) {
		return best( slots, quarters, true );
	}

	public static Optional<PriceWindow> mostExpensive( List<Price> slots, int quarters ) {
		return best( slots, quarters, false );
	}

	private static Optional<PriceWindow> best( List<Price> slots, int quarters, boolean cheapest ) {
		int count = slots.size( );
		if ( quarters < 1 || count < quarters ) {
			return Optional.empty( );
		}
		// KI-generiert: sum[i] = Summe der ersten i Preise; gaps[i] = Anzahl Lücken vor den Indizes 0..i-1
		double[] sum = new double[count + 1];
		int[] gaps = new int[count + 1];
		for ( int i = 0; i < count; i++ ) {
			sum[i + 1] = sum[i] + slots.get( i ).total( );
			boolean gap = i > 0 && !slots.get( i - 1 ).start( ).plus( PriceSchedule.QUARTER ).equals( slots.get( i ).start( ) );
			gaps[i + 1] = gaps[i] + ( gap ? 1 : 0 );
		}
		int bestStart = -1;
		double bestSum = 0;
		for ( int start = 0; start + quarters <= count; start++ ) {
			int end = start + quarters;
			// KI-generiert: Lücken innerhalb des Fensters liegen an den Indizes start+1 .. end-1
			if ( gaps[end] - gaps[start + 1] > 0 ) {
				continue;
			}
			double windowSum = sum[end] - sum[start];
			boolean better = cheapest ? windowSum < bestSum - EPSILON : windowSum > bestSum + EPSILON;
			if ( bestStart < 0 || better ) {
				bestStart = start;
				bestSum = windowSum;
			}
		}
		if ( bestStart < 0 ) {
			return Optional.empty( );
		}
		return Optional.of( new PriceWindow( slots.subList( bestStart, bestStart + quarters ), bestSum / quarters ) );
	}
}
