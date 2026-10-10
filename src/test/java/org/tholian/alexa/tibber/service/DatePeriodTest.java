package org.tholian.alexa.tibber.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** KI-generiert: AMAZON.DATE-Werte → Zeiträume (Berliner Kalendertage, ISO-Wochen Mo–So). */
class DatePeriodTest {

    @Test
    void parse_day() {
        assertThat(DatePeriod.parse("2026-10-09")).contains(
                new DatePeriod(DatePeriod.Kind.DAY, LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 10)));
    }

    @Test
    void parse_isoWeek_startsOnMonday() {
        assertThat(DatePeriod.parse("2026-W41")).contains(
                new DatePeriod(DatePeriod.Kind.WEEK, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12)));
    }

    @Test
    void parse_isoWeekAtYearBoundary_usesWeekBasedYear() {
        // KI-generiert: 2026 beginnt an einem Donnerstag → hat 53 ISO-Wochen; W01 beginnt bereits am 29.12.2025
        assertThat(DatePeriod.parse("2026-W01")).map(DatePeriod::start).contains(LocalDate.of(2025, 12, 29));
        assertThat(DatePeriod.parse("2026-W53")).map(DatePeriod::start).contains(LocalDate.of(2026, 12, 28));
        assertThat(DatePeriod.parse("2025-W53")).isEmpty();
    }

    @Test
    void parse_month() {
        assertThat(DatePeriod.parse("2026-09")).contains(
                new DatePeriod(DatePeriod.Kind.MONTH, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1)));
    }

    @Test
    void parse_year() {
        assertThat(DatePeriod.parse("2025")).contains(
                new DatePeriod(DatePeriod.Kind.YEAR, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"PRESENT_REF", "2026-WI", "2026-W41-WE", "202X", "XXXX-10-09", "2026-13", "2026-02-30", "gestern"})
    void parse_unsupportedValues_returnEmpty(String value) {
        assertThat(DatePeriod.parse(value)).isEmpty();
    }

    @Test
    void contains_isEndExclusive() {
        DatePeriod week = DatePeriod.parse("2026-W41").orElseThrow();

        assertThat(week.contains(LocalDate.of(2026, 10, 5))).isTrue();
        assertThat(week.contains(LocalDate.of(2026, 10, 11))).isTrue();
        assertThat(week.contains(LocalDate.of(2026, 10, 12))).isFalse();
    }
}
