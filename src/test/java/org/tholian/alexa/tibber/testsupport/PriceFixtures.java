package org.tholian.alexa.tibber.testsupport;

import org.tholian.alexa.tibber.model.Price;
import org.tholian.alexa.tibber.service.PriceSchedule;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * KI-generiert: Preis-Fixtures für Logik- und Handler-Tests. startsAt wird wie bei Tibber als ISO-8601 mit
 * Berliner Offset erzeugt; aufeinanderfolgende Werte liegen genau 15 Minuten auseinander.
 */
public final class PriceFixtures {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final Duration QUARTER = Duration.ofMinutes(15);

    private PriceFixtures() {
    }

    /** KI-generiert: Je ein Wert pro Viertelstunde ab {@code start}. */
    public static List<Price> quarters(Instant start, double... totals) {
        List<Price> prices = new ArrayList<>();
        for (int i = 0; i < totals.length; i++) {
            prices.add(price(start.plus(QUARTER.multipliedBy(i)), totals[i]));
        }
        return prices;
    }

    /** KI-generiert: Jeder Wert gilt für {@code quartersPerBlock} Viertelstunden (z. B. 4 = eine Stunde). */
    public static List<Price> blocks(Instant start, int quartersPerBlock, double... blockTotals) {
        double[] totals = new double[blockTotals.length * quartersPerBlock];
        for (int i = 0; i < totals.length; i++) {
            totals[i] = blockTotals[i / quartersPerBlock];
        }
        return quarters(start, totals);
    }

    public static Price price(Instant start, double total) {
        return price(start, total, "NORMAL");
    }

    public static Price price(Instant start, double total, String level) {
        return new Price(total, null, null, "EUR", level, start.atZone(BERLIN).toOffsetDateTime().toString());
    }

    public static PriceSchedule schedule(List<Price> slots) {
        return PriceSchedule.of(null, slots, null);
    }

    public static PriceSchedule schedule(Price current, List<Price> slots) {
        return PriceSchedule.of(current, slots, null);
    }
}
