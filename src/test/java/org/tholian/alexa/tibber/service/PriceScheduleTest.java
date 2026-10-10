package org.tholian.alexa.tibber.service;

import org.junit.jupiter.api.Test;
import org.tholian.alexa.tibber.model.Price;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.price;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.quarters;

/** KI-generiert: Viertelstunden-Fahrplan aus heute + morgen, inkl. Zeitumstellung und Stundenwerten. */
class PriceScheduleTest {

    private static final Instant T0 = Instant.parse("2026-10-10T08:00:00Z");
    private static final Duration QUARTER = Duration.ofMinutes(15);

    @Test
    void of_sortsSlotsAndTakesCurrencyFromCurrent() {
        List<Price> today = quarters(T0, 0.30, 0.20);
        Price current = new Price(0.30, null, null, "SEK", "NORMAL", today.get(0).startsAt());

        PriceSchedule schedule = PriceSchedule.of(current, List.of(today.get(1), today.get(0)), null);

        assertThat(schedule.slots()).extracting(Price::start).containsExactly(T0, T0.plus(QUARTER));
        assertThat(schedule.currency()).isEqualTo("SEK");
    }

    @Test
    void of_combinesTodayAndTomorrow() {
        PriceSchedule schedule = PriceSchedule.of(null, quarters(T0, 0.10), quarters(T0.plus(Duration.ofDays(1)), 0.20));

        assertThat(schedule.slots()).extracting(Price::total).containsExactly(0.10, 0.20);
    }

    @Test
    void of_withoutAnyCurrency_defaultsToEur() {
        Price withoutCurrency = new Price(0.30, null, null, null, "NORMAL", "2026-10-10T10:00:00+02:00");

        assertThat(PriceSchedule.of(null, List.of(withoutCurrency), null).currency()).isEqualTo("EUR");
    }

    @Test
    void of_withHourlyPrices_splitsIntoQuarters() {
        List<Price> hourly = List.of(price(T0, 0.30), price(T0.plus(Duration.ofHours(1)), 0.20),
                price(T0.plus(Duration.ofHours(2)), 0.10));

        PriceSchedule schedule = PriceSchedule.of(null, hourly, null);

        assertThat(schedule.slots()).hasSize(12);
        assertThat(schedule.slots().get(4).start()).isEqualTo(T0.plus(Duration.ofHours(1)));
        assertThat(schedule.slots().get(4).total()).isEqualTo(0.20);
        for (int i = 1; i < schedule.slots().size(); i++) {
            assertThat(Duration.between(schedule.slots().get(i - 1).start(), schedule.slots().get(i).start())).isEqualTo(QUARTER);
        }
    }

    @Test
    void at_returnsSlotContainingInstant() {
        PriceSchedule schedule = PriceSchedule.of(null, quarters(T0, 0.10, 0.20), null);

        assertThat(schedule.at(T0.plus(Duration.ofMinutes(20)))).map(Price::total).contains(0.20);
        assertThat(schedule.at(T0.plus(Duration.ofMinutes(30)))).isEmpty();
        assertThat(schedule.at(T0.minusSeconds(1))).isEmpty();
    }

    @Test
    void from_startsWithRunningQuarter() {
        PriceSchedule schedule = PriceSchedule.of(null, quarters(T0, 0.10, 0.20, 0.30), null);

        assertThat(schedule.from(T0.plus(Duration.ofMinutes(20)))).extracting(Price::total).containsExactly(0.20, 0.30);
    }

    @Test
    void day_returnsOnlySlotsOfBerlinDate() {
        // KI-generiert: 21:30 UTC = 23:30 Uhr in Berlin – die letzten zwei Viertelstunden gehören schon zum 11.10.
        PriceSchedule schedule = PriceSchedule.of(null, quarters(Instant.parse("2026-10-10T21:30:00Z"), 0.1, 0.2, 0.3, 0.4), null);

        assertThat(schedule.day(LocalDate.of(2026, 10, 10))).extracting(Price::total).containsExactly(0.1, 0.2);
        assertThat(schedule.day(LocalDate.of(2026, 10, 11))).extracting(Price::total).containsExactly(0.3, 0.4);
    }

    @Test
    void day_onDstChange_has100Quarters() {
        // KI-generiert: 25.10.2026 00:00 CEST = 24.10. 22:00 UTC; der Tag dauert 25 Stunden (Rückstellung 03:00 → 02:00)
        double[] totals = new double[100];
        java.util.Arrays.fill(totals, 0.25);
        PriceSchedule schedule = PriceSchedule.of(null, quarters(Instant.parse("2026-10-24T22:00:00Z"), totals), null);

        assertThat(schedule.day(LocalDate.of(2026, 10, 25))).hasSize(100);
        assertThat(schedule.day(LocalDate.of(2026, 10, 26))).isEmpty();
    }

    @Test
    void lastSlot_returnsLatestQuarter() {
        PriceSchedule schedule = PriceSchedule.of(null, quarters(T0, 0.10, 0.20), null);

        assertThat(schedule.lastSlot()).map(Price::total).contains(0.20);
        assertThat(PriceSchedule.of(null, List.of(), null).lastSlot()).isEmpty();
    }
}
