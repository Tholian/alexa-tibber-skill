package org.tholian.alexa.tibber.service;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * KI-generiert: Zeitraum aus einem AMAZON.DATE-Slotwert als Kalendertage {@code [start, endExclusive)}.
 * Unterstützt: Tag („2026-10-09“), ISO-Woche Mo–So („2026-W41“), Monat („2026-09“), Jahr („2026“).
 * Nicht unterstützt (→ leer): Wochenende („-WE“), Jahreszeit, Jahrzehnt, {@code PRESENT_REF}, {@code XXXX}-Platzhalter.
 * <p>
 * SICHERHEIT: Der Slotwert ist Nutzereingabe; er wird nur über feste Muster geparst.
 */
public record DatePeriod( Kind kind, LocalDate start, LocalDate endExclusive ) {

	public enum Kind { DAY, WEEK, MONTH, YEAR }

	private static final Pattern DAY = Pattern.compile( "\\d{4}-\\d{2}-\\d{2}" );
	private static final Pattern WEEK = Pattern.compile( "(\\d{4})-W(\\d{1,2})" );
	private static final Pattern MONTH = Pattern.compile( "\\d{4}-\\d{2}" );
	private static final Pattern YEAR = Pattern.compile( "\\d{4}" );

	public static Optional<DatePeriod> parse( @Nullable String value ) {
		if ( value == null ) {
			return Optional.empty( );
		}
		String text = value.strip( );
		try {
			if ( DAY.matcher( text ).matches( ) ) {
				LocalDate day = LocalDate.parse( text );
				return Optional.of( new DatePeriod( Kind.DAY, day, day.plusDays( 1 ) ) );
			}
			Matcher week = WEEK.matcher( text );
			if ( week.matches( ) ) {
				return isoWeek( Integer.parseInt( week.group( 1 ) ), Integer.parseInt( week.group( 2 ) ) );
			}
			if ( MONTH.matcher( text ).matches( ) ) {
				YearMonth month = YearMonth.parse( text );
				return Optional.of( new DatePeriod( Kind.MONTH, month.atDay( 1 ), month.plusMonths( 1 ).atDay( 1 ) ) );
			}
			if ( YEAR.matcher( text ).matches( ) ) {
				int year = Integer.parseInt( text );
				return Optional.of( new DatePeriod( Kind.YEAR, LocalDate.of( year, 1, 1 ), LocalDate.of( year + 1, 1, 1 ) ) );
			}
		} catch ( DateTimeException e ) {
			// KI-generiert: ungültige Kalenderwerte wie „2026-02-30“ oder „2026-13“
			return Optional.empty( );
		}
		return Optional.empty( );
	}

	/** KI-generiert: Liegt {@code date} im Zeitraum (Ende exklusiv)? */
	public boolean contains( LocalDate date ) {
		return !date.isBefore( start ) && date.isBefore( endExclusive );
	}

	private static Optional<DatePeriod> isoWeek( int weekBasedYear, int week ) {
		// KI-generiert: Der 4. Januar liegt immer in ISO-Woche 1 des Wochenjahres
		LocalDate reference = LocalDate.of( weekBasedYear, 1, 4 );
		if ( !IsoFields.WEEK_OF_WEEK_BASED_YEAR.rangeRefinedBy( reference ).isValidValue( week ) ) {
			return Optional.empty( );
		}
		LocalDate monday = reference.with( IsoFields.WEEK_OF_WEEK_BASED_YEAR, week ).with( DayOfWeek.MONDAY );
		return Optional.of( new DatePeriod( Kind.WEEK, monday, monday.plusWeeks( 1 ) ) );
	}
}
