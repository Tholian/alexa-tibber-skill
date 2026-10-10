package org.tholian.alexa.tibber.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tholian.alexa.tibber.testsupport.PriceFixtures.price;

/**
 * KI-generiert: Sprachtexte des Skills. Uhr fest auf 2026-10-10T08:00:00Z = Samstag, 10:00 Uhr in Berlin (CEST).
 */
class SpeechFormatterTest {

    private static final Instant NOW = Instant.parse("2026-10-10T08:00:00Z");

    private final SpeechFormatter formatter = new SpeechFormatter(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void today_usesBerlinDate() {
        // KI-generiert: 22:30 UTC = 00:30 Uhr am Folgetag in Berlin
        var lateEvening = new SpeechFormatter(Clock.fixed(Instant.parse("2026-10-10T22:30:00Z"), ZoneOffset.UTC));

        assertThat(lateEvening.today()).isEqualTo(LocalDate.of(2026, 10, 11));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "NULL", value = {
            "0.234 | EUR  | 23,4 Cent",
            "0.234 | NULL | 23,4 Cent",
            "0.234 | SEK  | 23,4 Öre",
            "0.234 | NOK  | 23,4 Öre",
            "0.234 | usd  | 0,23 USD",
            "0.25  | EUR  | 25,0 Cent"
    })
    void unitPrice_usesSubunitOfCurrency(double total, String currency, String expected) {
        assertThat(formatter.unitPrice(total, currency)).isEqualTo(expected);
    }

    @Test
    void unitPrice_negative_keepsSign() {
        assertThat(formatter.unitPrice(-0.02, "EUR")).isEqualTo("-2,0 Cent");
    }

    @Test
    void unitPrice_roundingToNegativeZero_isPlainZero() {
        assertThat(formatter.unitPrice(-0.0004, "EUR")).isEqualTo("0,0 Cent");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "NULL", value = {
            "2.31 | EUR  | 2,31 Euro",
            "2.31 | NULL | 2,31 Euro",
            "2.31 | SEK  | 2,31 Kronen",
            "2.31 | DKK  | 2,31 Kronen",
            "2.31 | CHF  | 2,31 CHF"
    })
    void money_usesMainUnitOfCurrency(double amount, String currency, String expected) {
        assertThat(formatter.money(amount, currency)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "8.44   | 8,44 Kilowattstunden",
            "8.0    | 8 Kilowattstunden",
            "1.0    | 1 Kilowattstunde",
            "1.004  | 1 Kilowattstunde",
            "0.84   | 0,84 Kilowattstunden",
            "0.0    | 0 Kilowattstunden",
            "12.34  | 12,3 Kilowattstunden",
            "312.5  | 312,5 Kilowattstunden",
            "-0.001 | 0 Kilowattstunden",
            "8.4    | 8,4 Kilowattstunden"
    })
    void energy_formatsKilowattHours(double kwh, String expected) {
        assertThat(formatter.energy(kwh)).isEqualTo(expected);
    }

    @Test
    void power_roundsToWholeWatts() {
        assertThat(formatter.power(1239.6)).isEqualTo("1240 Watt");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "180 | 3 Stunden",
            "60  | eine Stunde",
            "90  | eine Stunde und 30 Minuten",
            "135 | 2 Stunden und 15 Minuten",
            "45  | 45 Minuten"
    })
    void duration_speaksHoursAndMinutes(int minutes, String expected) {
        assertThat(formatter.duration(minutes)).isEqualTo(expected);
    }

    @Test
    void clockTime_convertsToBerlinTime() {
        assertThat(formatter.clockTime("2026-10-10T21:15:00Z")).isEqualTo("23:15 Uhr");
    }

    @Test
    void clockTime_inWinter_usesCet() {
        assertThat(formatter.clockTime("2026-01-15T11:00:00Z")).isEqualTo("12:00 Uhr");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "2026-10-10T21:15:00Z      | heute um 23:15 Uhr",
            "2026-10-11T03:15:00+02:00 | morgen um 03:15 Uhr",
            "2026-10-12T08:00:00Z      | am 12. Oktober um 10:00 Uhr"
    })
    void time_addsDayLabel(String startsAt, String expected) {
        assertThat(formatter.time(startsAt)).isEqualTo(expected);
    }

    @Test
    void dayLabel_namesYesterday() {
        assertThat(formatter.dayLabel(LocalDate.of(2026, 10, 9))).isEqualTo("gestern");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "NULL", value = {
            "VERY_CHEAP     | sehr günstig",
            "CHEAP          | günstig",
            "NORMAL         | normal",
            "EXPENSIVE      | teuer",
            "VERY_EXPENSIVE | sehr teuer",
            "SOMETHING_NEW  | something_new",
            "NULL           | unbekannt"
    })
    void level_mapsTibberLevels(String level, String expected) {
        assertThat(formatter.level(level)).isEqualTo(expected);
    }

    @Test
    void capitalize_uppercasesFirstLetter() {
        assertThat(SpeechFormatter.capitalize("heute")).isEqualTo("Heute");
        assertThat(SpeechFormatter.capitalize("")).isEmpty();
    }

    @Test
    void startOf_runningSlot_saysSofort() {
        assertThat(formatter.startOf(price(NOW.minusSeconds(60), 0.10), NOW)).isEqualTo("sofort");
    }

    @Test
    void startOf_futureSlot_saysTimeWithDay() {
        assertThat(formatter.startOf(price(Instant.parse("2026-10-10T21:15:00Z"), 0.10), NOW)).isEqualTo("heute um 23:15 Uhr");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "2026-10-10 | heute",
            "2026-10-09 | gestern",
            "2026-10-03 | am 3. Oktober",
            "2026-W41   | diese Woche",
            "2026-W40   | letzte Woche",
            "2026-W39   | in der Woche ab dem 21. September",
            "2026-10    | diesen Monat",
            "2026-09    | im September",
            "2025-09    | im September 2025",
            "2026       | dieses Jahr",
            "2025       | letztes Jahr",
            "2023       | im Jahr 2023"
    })
    void period_namesPeriodRelativeToToday(String amazonDate, String expected) {
        assertThat(formatter.period(DatePeriod.parse(amazonDate).orElseThrow())).isEqualTo(expected);
    }

    @Test
    void coverage_namesIncompletePeriods() {
        assertThat(formatter.coverage(EnergyResult.Coverage.COMPLETE)).isEmpty();
        assertThat(formatter.coverage(EnergyResult.Coverage.SO_FAR_TODAY)).isEqualTo("bisher ");
        assertThat(formatter.coverage(EnergyResult.Coverage.UNTIL_YESTERDAY)).isEqualTo("bis gestern ");
    }
}
