package org.tholian.alexa.tibber.service;

import org.jspecify.annotations.Nullable;

/**
 * KI-generiert: Ergebnis einer Zeitraum-Abfrage.
 * {@link Total#amount()} ist bei Verbrauch die Kosten, bei Einspeisung der Erlös; {@code null}, wenn Tibber
 * nicht für alle Werte einen Betrag liefert.
 */
public sealed interface EnergyResult {

	/** KI-generiert: Vollständigkeit – abgeschlossen, „heute bisher“ oder „bis gestern“ (laufender Zeitraum). */
	enum Coverage { COMPLETE, SO_FAR_TODAY, UNTIL_YESTERDAY }

	record Total( double energyKwh, @Nullable Double amount, String currency, Coverage coverage ) implements EnergyResult {
	}

	record NoData( ) implements EnergyResult {
	}

	record Future( ) implements EnergyResult {
	}

	record TooOld( ) implements EnergyResult {
	}
}
