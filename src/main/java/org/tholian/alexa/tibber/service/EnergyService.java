package org.tholian.alexa.tibber.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;

import org.tholian.alexa.tibber.model.TibberResponse;

/**
 * KI-generiert: Verbrauch bzw. Einspeisung für einen Zeitraum (Tag, Woche, Monat, Jahr) als Summe.
 * <p>
 * Abfragestrategie (Zeitzone Europe/Berlin):
 * <ul>
 *   <li>heute → Stundenwerte seit 00:00 („bisher“)</li>
 *   <li>laufende Woche / laufender Monat → Tageswerte bis gestern</li>
 *   <li>laufendes Jahr → abgeschlossene Monate + Tage des laufenden Monats bis gestern (zwei Abfragen)</li>
 *   <li>vergangene Tage/Wochen/Monate bis 400 Tage zurück → Tageswerte; ältere Monate → Monatswerte;
 *       vergangene Jahre → Jahreswerte; noch älter → {@link EnergyResult.TooOld}</li>
 * </ul>
 * {@code last} ist je Auflösung um 1 größer als nötig, falls Tibber den angebrochenen Zeitraum mitliefert
 * (bei Stundenwerten 25 wegen des Zeitumstellungstags);
 * die Zuordnung zum Zeitraum erfolgt immer über den Beginn ({@code from}) jedes Werts.
 * <p>
 * SICHERHEIT: Auflösung und Anzahl gehen als GraphQL-Variablen an Tibber, nie per String-Verkettung.
 * Datensparsamkeit: nur Energiemengen, Beträge und Währung.
 */
public class EnergyService {

	static final int MAX_DAYS = 400;
	static final int MAX_MONTHS = 120;
	static final int MAX_YEARS = 20;
	private static final int HOURS_OF_TODAY = 25;

	@Language( "GraphQL" )
	static final String CONSUMPTION_QUERY = """
			query($resolution: EnergyResolution!, $last: Int!) {
			  viewer {
			    homes {
			      consumption(resolution: $resolution, last: $last) {
			        nodes { from to consumption cost currency }
			      }
			    }
			  }
			}
			""";

	@Language( "GraphQL" )
	static final String PRODUCTION_QUERY = """
			query($resolution: EnergyResolution!, $last: Int!) {
			  viewer {
			    homes {
			      production(resolution: $resolution, last: $last) {
			        nodes { from to production profit currency }
			      }
			    }
			  }
			}
			""";

	private final TibberGraphQlClient client;
	private final Clock clock;

	public EnergyService( TibberGraphQlClient client, Clock clock ) {
		this.client = client;
		this.clock = clock;
	}

	/**
	 * @throws TibberAuthenticationException wenn Tibber den Token ablehnt
	 * @throws TibberApiException            bei sonstigen Fehlern
	 */
	public EnergyResult total( String token, DatePeriod period, EnergyKind kind ) {
		LocalDate today = LocalDate.now( clock.withZone( SpeechFormatter.BERLIN ) );
		// KI-generiert: Alexa bricht nach 8 s ab. Das laufende Jahr braucht zwei Abfragen – sie teilen sich ein
		// Zeitbudget (ein Request-Timeout), statt je Abfrage den vollen Timeout auszuschöpfen.
		Deadline deadline = Deadline.after( client.requestTimeout( ) );
		if ( period.start( ).isAfter( today ) ) {
			return new EnergyResult.Future( );
		}
		if ( period.contains( today ) ) {
			return current( token, period, kind, today, deadline );
		}
		return past( token, period, kind, today, deadline );
	}

	private EnergyResult current( String token, DatePeriod period, EnergyKind kind, LocalDate today, Deadline deadline ) {
		if ( period.kind( ) == DatePeriod.Kind.YEAR ) {
			int completedMonths = today.getMonthValue( ) - 1;
			int completedDaysThisMonth = today.getDayOfMonth( ) - 1;
			if ( completedMonths == 0 && completedDaysThisMonth == 0 ) {
				return today( token, kind, today, deadline );
			}
			LocalDate firstOfMonth = today.withDayOfMonth( 1 );
			List<EnergyNode> nodes = new ArrayList<>( );
			if ( completedMonths > 0 ) {
				nodes.addAll( within( fetch( token, kind, "MONTHLY", completedMonths + 1, deadline ), period.start( ), firstOfMonth ) );
			}
			if ( completedDaysThisMonth > 0 ) {
				nodes.addAll( within( fetch( token, kind, "DAILY", completedDaysThisMonth + 1, deadline ), firstOfMonth, today ) );
			}
			return sum( nodes, EnergyResult.Coverage.UNTIL_YESTERDAY );
		}
		int completedDays = (int) ChronoUnit.DAYS.between( period.start( ), today );
		if ( completedDays == 0 ) {
			return today( token, kind, today, deadline );
		}
		return sum( within( fetch( token, kind, "DAILY", completedDays + 1, deadline ), period.start( ), today ),
				EnergyResult.Coverage.UNTIL_YESTERDAY );
	}

	private EnergyResult past( String token, DatePeriod period, EnergyKind kind, LocalDate today, Deadline deadline ) {
		if ( period.kind( ) == DatePeriod.Kind.YEAR ) {
			int years = today.getYear( ) - period.start( ).getYear( );
			return years > MAX_YEARS ? new EnergyResult.TooOld( ) : complete( fetch( token, kind, "ANNUAL", years + 1, deadline ), period );
		}
		long days = ChronoUnit.DAYS.between( period.start( ), today );
		if ( days <= MAX_DAYS ) {
			return complete( fetch( token, kind, "DAILY", (int) days + 1, deadline ), period );
		}
		if ( period.kind( ) == DatePeriod.Kind.MONTH ) {
			long months = ChronoUnit.MONTHS.between( YearMonth.from( period.start( ) ), YearMonth.from( today ) );
			if ( months <= MAX_MONTHS ) {
				return complete( fetch( token, kind, "MONTHLY", (int) months + 1, deadline ), period );
			}
		}
		return new EnergyResult.TooOld( );
	}

	private EnergyResult today( String token, EnergyKind kind, LocalDate today, Deadline deadline ) {
		return sum( within( fetch( token, kind, "HOURLY", HOURS_OF_TODAY, deadline ), today, today.plusDays( 1 ) ),
				EnergyResult.Coverage.SO_FAR_TODAY );
	}

	private static EnergyResult complete( List<EnergyNode> nodes, DatePeriod period ) {
		return sum( within( nodes, period.start( ), period.endExclusive( ) ), EnergyResult.Coverage.COMPLETE );
	}

	private List<EnergyNode> fetch( String token, EnergyKind kind, String resolution, int last, Deadline deadline ) {
		Map<String, Object> variables = Map.of( "resolution", resolution, "last", last );
		String query = kind == EnergyKind.CONSUMPTION ? CONSUMPTION_QUERY : PRODUCTION_QUERY;
		TibberResponse.Home home = TibberGraphQlClient.firstHome( client.query( token, query, variables, deadline.remaining( ) ) );
		if ( kind == EnergyKind.CONSUMPTION ) {
			TibberResponse.Consumption consumption = home.consumption( );
			if ( consumption == null || consumption.nodes( ) == null ) {
				return List.of( );
			}
			return consumption.nodes( ).stream( )
					.map( node -> new EnergyNode( instant( node.from( ) ), node.consumption( ), node.cost( ), node.currency( ) ) )
					.toList( );
		}
		TibberResponse.Production production = home.production( );
		if ( production == null || production.nodes( ) == null ) {
			return List.of( );
		}
		return production.nodes( ).stream( )
				.map( node -> new EnergyNode( instant( node.from( ) ), node.production( ), node.profit( ), node.currency( ) ) )
				.toList( );
	}

	/** KI-generiert: Werte, deren Beginn im Zeitraum {@code [from, toExclusive)} (Berliner Kalendertage) liegt. */
	private static List<EnergyNode> within( List<EnergyNode> nodes, LocalDate from, LocalDate toExclusive ) {
		Instant start = from.atStartOfDay( SpeechFormatter.BERLIN ).toInstant( );
		Instant end = toExclusive.atStartOfDay( SpeechFormatter.BERLIN ).toInstant( );
		return nodes.stream( ).filter( node -> !node.from( ).isBefore( start ) && node.from( ).isBefore( end ) ).toList( );
	}

	private static EnergyResult sum( List<EnergyNode> nodes, EnergyResult.Coverage coverage ) {
		List<EnergyNode> measured = nodes.stream( ).filter( node -> node.energy( ) != null ).toList( );
		if ( measured.isEmpty( ) ) {
			return new EnergyResult.NoData( );
		}
		double energy = measured.stream( ).mapToDouble( EnergyNode::energy ).sum( );
		boolean amountComplete = measured.stream( ).allMatch( node -> node.amount( ) != null );
		@Nullable Double amount = amountComplete ? measured.stream( ).mapToDouble( EnergyNode::amount ).sum( ) : null;
		String currency = measured.stream( )
				.map( EnergyNode::currency )
				.filter( Objects::nonNull )
				.findFirst( )
				.orElse( "EUR" );
		return new EnergyResult.Total( energy, amount, currency, coverage );
	}

	private static Instant instant( @Nullable String isoDateTime ) {
		// KI-generiert: Fehlendes oder unlesbares {@code from} als Tibber-Fehler melden; der Wert steht bewusst nicht in der Message
		try {
			if ( isoDateTime != null ) {
				return OffsetDateTime.parse( isoDateTime ).toInstant( );
			}
		} catch ( DateTimeParseException e ) {
			// fällt durch zur Exception unten
		}
		throw new TibberApiException( "Tibber-Antwort unlesbar" );
	}

	/** KI-generiert: Gemeinsame Sicht auf Verbrauchs- und Einspeisewerte. */
	private record EnergyNode( Instant from, @Nullable Double energy, @Nullable Double amount, @Nullable String currency ) {
	}
}
