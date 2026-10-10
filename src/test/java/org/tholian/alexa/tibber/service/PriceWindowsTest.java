package org.tholian.alexa.tibber.service;

import org.junit.jupiter.api.Test;
import org.tholian.alexa.tibber.model.Price;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.quarters;

/** KI-generiert: Günstigstes/teuerstes zusammenhängendes Fenster aus N Viertelstunden. */
class PriceWindowsTest {

    private static final Instant T0 = Instant.parse("2026-10-10T08:00:00Z");

    @Test
    void cheapest_findsLowestAverageWindow() {
        List<Price> slots = quarters(T0, 0.30, 0.10, 0.10, 0.30, 0.05, 0.40);

        PriceWindow window = PriceWindows.cheapest(slots, 2).orElseThrow();

        assertThat(window.first().start()).isEqualTo(T0.plus(Duration.ofMinutes(15)));
        assertThat(window.average()).isCloseTo(0.10, within(1e-9));
        assertThat(window.end()).isEqualTo(T0.plus(Duration.ofMinutes(45)));
    }

    @Test
    void cheapest_onTie_returnsEarliest() {
        PriceWindow window = PriceWindows.cheapest(quarters(T0, 0.20, 0.20, 0.20, 0.20), 2).orElseThrow();

        assertThat(window.first().start()).isEqualTo(T0);
    }

    @Test
    void mostExpensive_findsHighestAverageWindow() {
        List<Price> slots = quarters(T0, 0.30, 0.10, 0.10, 0.30, 0.05, 0.40);

        PriceWindow window = PriceWindows.mostExpensive(slots, 2).orElseThrow();

        assertThat(window.first().start()).isEqualTo(T0.plus(Duration.ofMinutes(60)));
        assertThat(window.average()).isCloseTo(0.225, within(1e-9));
    }

    @Test
    void window_doesNotSpanGap() {
        List<Price> slots = new ArrayList<>(quarters(T0, 0.10, 0.10));
        slots.addAll(quarters(T0.plus(Duration.ofHours(1)), 0.10, 0.10));

        assertThat(PriceWindows.cheapest(slots, 3)).isEmpty();
        assertThat(PriceWindows.cheapest(slots, 2)).map(w -> w.first().start()).contains(T0);
    }

    @Test
    void tooFewSlotsOrInvalidLength_returnsEmpty() {
        assertThat(PriceWindows.cheapest(quarters(T0, 0.10), 2)).isEmpty();
        assertThat(PriceWindows.cheapest(quarters(T0, 0.10, 0.20), 0)).isEmpty();
        assertThat(PriceWindows.mostExpensive(List.of(), 1)).isEmpty();
    }

    @Test
    void cheapest_withNegativePrices_picksLowestSum() {
        PriceWindow window = PriceWindows.cheapest(quarters(T0, 0.05, -0.02, -0.03, 0.10), 2).orElseThrow();

        assertThat(window.first().start()).isEqualTo(T0.plus(Duration.ofMinutes(15)));
        assertThat(window.average()).isCloseTo(-0.025, within(1e-9));
    }

    @Test
    void cheapest_acrossDstChange_staysContiguous() {
        // KI-generiert: 25.10.2026 00:00 UTC = 02:00 CEST; um 01:00 UTC wird auf 02:00 CET zurückgestellt
        double[] totals = new double[8];
        Arrays.fill(totals, 0.10);
        Instant start = Instant.parse("2026-10-25T00:00:00Z");

        PriceWindow window = PriceWindows.cheapest(quarters(start, totals), 8).orElseThrow();

        assertThat(window.first().start()).isEqualTo(start);
        assertThat(window.end()).isEqualTo(start.plus(Duration.ofHours(2)));
    }
}
