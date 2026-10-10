package org.tholian.alexa.tibber.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.tholian.alexa.tibber.model.Price;

/**
 * KI-generiert: Alle sprachlichen Formatierungen des Skills an einer Stelle – Beträge, Energie, Leistung,
 * Dauer, Uhrzeiten mit Tagesangabe und Preisniveaus. Zeitzone ist immer Europe/Berlin; „heute“ und „morgen“
 * beziehen sich auf die injizierte Uhr.
 * <p>
 * Währungen: EUR → Cent/Euro, SEK/NOK/DKK → Öre/Kronen (der Tibber-Demo-Account liefert SEK), sonst ISO-Code.
 * Ohne Währungsangabe gilt EUR (Tibber Deutschland).
 */
public class SpeechFormatter {

	public static final ZoneId BERLIN = ZoneId.of( "Europe/Berlin" );

	private static final Set<String> KRONEN = Set.of( "SEK", "NOK", "DKK" );
	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern( "HH:mm", Locale.GERMAN );
	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern( "d. MMMM", Locale.GERMAN );

	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern( "MMMM", Locale.GERMAN );

	private final Clock clock;

	public SpeechFormatter( Clock clock ) {
		this.clock = clock;
	}

	/** KI-generiert: Heutiges Datum in Berlin. */
	public LocalDate today( ) {
		return LocalDate.now( clock.withZone( BERLIN ) );
	}

	/** KI-generiert: Preis pro Kilowattstunde in der Unterwährung (Cent/Öre), eine Nachkommastelle. */
	public String unitPrice( double total, @Nullable String currency ) {
		String code = code( currency );
		if ( "EUR".equals( code ) ) {
			return decimal1( total * 100 ) + " Cent";
		}
		if ( KRONEN.contains( code ) ) {
			return decimal1( total * 100 ) + " Öre";
		}
		return String.format( Locale.GERMAN, "%.2f %s", total, code );
	}

	/** KI-generiert: Geldbetrag in der Hauptwährung (Euro/Kronen), zwei Nachkommastellen. */
	public String money( double amount, @Nullable String currency ) {
		String code = code( currency );
		String value = String.format( Locale.GERMAN, "%.2f", amount );
		if ( "EUR".equals( code ) ) {
			return value + " Euro";
		}
		if ( KRONEN.contains( code ) ) {
			return value + " Kronen";
		}
		return value + " " + code;
	}

	/**
	 * KI-generiert: Energiemenge – unter 10 kWh zwei, sonst eine Nachkommastelle; abschließende Nullen entfallen,
	 * 1 kWh im Singular, nie „-0“.
	 */
	public String energy( double kwh ) {
		String raw = String.format( Locale.GERMAN, Math.abs( kwh ) < 10 ? "%.2f" : "%.1f", kwh );
		String value = trimZeros( raw );
		if ( "-0".equals( value ) ) {
			value = "0";
		}
		return "1".equals( value ) ? "1 Kilowattstunde" : value + " Kilowattstunden";
	}

	/** KI-generiert: Leistung in ganzen Watt. */
	public String power( double watts ) {
		return Math.round( watts ) + " Watt";
	}

	/** KI-generiert: Dauer in Stunden und Minuten, z. B. „eine Stunde und 30 Minuten“. */
	public String duration( int minutes ) {
		int hours = minutes / 60;
		int rest = minutes % 60;
		if ( hours == 0 ) {
			return rest + " Minuten";
		}
		String hoursText = hours == 1 ? "eine Stunde" : hours + " Stunden";
		return rest == 0 ? hoursText : hoursText + " und " + rest + " Minuten";
	}

	/** KI-generiert: Uhrzeit in Berlin ohne Tagesangabe, z. B. „23:15 Uhr“. */
	public String clockTime( String startsAt ) {
		return zoned( startsAt ).format( CLOCK ) + " Uhr";
	}

	/** KI-generiert: Uhrzeit mit Tagesangabe, z. B. „morgen um 03:15 Uhr“. */
	public String time( String startsAt ) {
		ZonedDateTime time = zoned( startsAt );
		return dayLabel( time.toLocalDate( ) ) + " um " + time.format( CLOCK ) + " Uhr";
	}

	/** KI-generiert: Beginn eines Zeitfensters – „sofort“, wenn es bereits läuft, sonst Uhrzeit mit Tagesangabe. */
	public String startOf( Price slot, Instant now ) {
		return slot.start( ).isAfter( now ) ? time( slot.startsAt( ) ) : "sofort";
	}

	/** KI-generiert: „heute“, „morgen“, „gestern“ oder „am 12. Oktober“. */
	public String dayLabel( LocalDate date ) {
		LocalDate today = today( );
		if ( date.equals( today ) ) {
			return "heute";
		}
		if ( date.equals( today.plusDays( 1 ) ) ) {
			return "morgen";
		}
		if ( date.equals( today.minusDays( 1 ) ) ) {
			return "gestern";
		}
		return "am " + date.format( DAY_MONTH );
	}

	/** KI-generiert: Zeitraum relativ zu heute, z. B. „gestern“, „letzte Woche“, „im September“, „dieses Jahr“. */
	public String period( DatePeriod period ) {
		LocalDate today = today( );
		LocalDate start = period.start( );
		return switch ( period.kind( ) ) {
			case DAY -> dayLabel( start );
			case WEEK -> period.contains( today ) ? "diese Woche"
			             : start.equals( today.with( DayOfWeek.MONDAY ).minusWeeks( 1 ) ) ? "letzte Woche"
			             : "in der Woche ab dem " + start.format( DAY_MONTH );
			case MONTH -> period.contains( today ) ? "diesen Monat"
			              : "im " + start.format( MONTH ) + ( start.getYear( ) == today.getYear( ) ? "" : " " + start.getYear( ) );
			case YEAR -> period.contains( today ) ? "dieses Jahr"
			             : start.getYear( ) == today.getYear( ) - 1 ? "letztes Jahr"
			             : "im Jahr " + start.getYear( );
		};
	}

	/** KI-generiert: Einschub für unvollständige Zeiträume – „bisher “, „bis gestern “ oder leer. */
	public String coverage( EnergyResult.Coverage coverage ) {
		return switch ( coverage ) {
			case COMPLETE -> "";
			case SO_FAR_TODAY -> "bisher ";
			case UNTIL_YESTERDAY -> "bis gestern ";
		};
	}

	/** KI-generiert: Tibber-Preisniveau auf Deutsch; unbekannte Werte klein geschrieben, {@code null} = „unbekannt“. */
	public String level( @Nullable String level ) {
		if ( level == null ) {
			return "unbekannt";
		}
		return switch ( level ) {
			case "VERY_CHEAP" -> "sehr günstig";
			case "CHEAP" -> "günstig";
			case "NORMAL" -> "normal";
			case "EXPENSIVE" -> "teuer";
			case "VERY_EXPENSIVE" -> "sehr teuer";
			default -> level.toLowerCase( Locale.GERMAN );
		};
	}

	/** KI-generiert: Erster Buchstabe groß – für Sätze, die mit „heute“, „morgen“ usw. beginnen. */
	public static String capitalize( String text ) {
		return text.isEmpty( ) ? text : Character.toUpperCase( text.charAt( 0 ) ) + text.substring( 1 );
	}

	private static String code( @Nullable String currency ) {
		return currency == null || currency.isBlank( ) ? "EUR" : currency.strip( ).toUpperCase( Locale.ROOT );
	}

	private static String decimal1( double value ) {
		String text = String.format( Locale.GERMAN, "%.1f", value );
		// KI-generiert: Werte, die auf „-0,0“ runden, als „0,0“ ausgeben
		return "-0,0".equals( text ) ? "0,0" : text;
	}

	/** KI-generiert: Entfernt abschließende Nullen nach dem Komma (und das Komma selbst, wenn nichts übrig bleibt). */
	private static String trimZeros( String value ) {
		if ( !value.contains( "," ) ) {
			return value;
		}
		String trimmed = value.replaceAll( "0+$", "" );
		return trimmed.endsWith( "," ) ? trimmed.substring( 0, trimmed.length( ) - 1 ) : trimmed;
	}

	private static ZonedDateTime zoned( String startsAt ) {
		return OffsetDateTime.parse( startsAt ).atZoneSameInstant( BERLIN );
	}
}
