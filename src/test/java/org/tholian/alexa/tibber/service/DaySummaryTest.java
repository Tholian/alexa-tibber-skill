package org.tholian.alexa.tibber.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.quarters;

/** KI-generiert: Minimum, Maximum und Durchschnitt einer Viertelstundenliste. */
class DaySummaryTest {

    private static final Instant T0 = Instant.parse("2026-10-10T08:00:00Z");

    @Test
    void of_returnsMinMaxAndAverage() {
        DaySummary summary = DaySummary.of(quarters(T0, 0.30, 0.10, 0.20)).orElseThrow();

        assertThat(summary.min().start()).isEqualTo(T0.plus(Duration.ofMinutes(15)));
        assertThat(summary.max().start()).isEqualTo(T0);
        assertThat(summary.average()).isCloseTo(0.20, within(1e-9));
    }

    @Test
    void of_onTie_takesEarliest() {
        DaySummary summary = DaySummary.of(quarters(T0, 0.10, 0.10)).orElseThrow();

        assertThat(summary.min().start()).isEqualTo(T0);
        assertThat(summary.max().start()).isEqualTo(T0);
    }

    @Test
    void of_empty_returnsEmpty() {
        assertThat(DaySummary.of(List.of())).isEmpty();
    }
}
