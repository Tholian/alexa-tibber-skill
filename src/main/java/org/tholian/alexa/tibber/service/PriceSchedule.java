package org.tholian.alexa.tibber.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.tholian.alexa.tibber.model.Price;

/**
 * KI-generiert: Bekannte Preise (heute + morgen) als aufsteigend sortierte Viertelstunden.
 * Liefert Tibber Stundenwerte, wird jeder Wert in vier gleiche Viertelstunden aufgeteilt – die Preislogik
 * rechnet dadurch immer auf dem 15-Minuten-Raster.
 * <p>
 * Alle Vergleiche laufen über {@link Instant}, nie über Listenpositionen: Am Tag der Zeitumstellung im Herbst
 * hat ein Tag 100 statt 96 Viertelstunden.
 *
 * @param current  Preis der laufenden Viertelstunde laut Tibber (kann fehlen)
 * @param slots    Viertelstunden, aufsteigend nach Beginn
 * @param currency Währung (aus den Preisen, sonst EUR)
 */
public record PriceSchedule( @Nullable Price current, List<Price> slots, String currency ) {

	public static final Duration QUARTER = Duration.ofMinutes( 15 );
	private static final Duration HOUR = Duration.ofHours( 1 );

	public PriceSchedule {
		slots = List.copyOf( slots );
	}

	/** KI-generiert: Baut den Fahrplan aus der Tibber-Antwort ({@code tomorrow} ist vor ca. 13 Uhr leer oder null). */
	public static PriceSchedule of( @Nullable Price current, List<Price> today, @Nullable List<Price> tomorrow ) {
		List<Price> all = new ArrayList<>( today );
		if ( tomorrow != null ) {
			all.addAll( tomorrow );
		}
		all.sort( Comparator.comparing( Price::start ) );
		List<Price> quarters = isHourly( all ) ? splitHours( all ) : all;
		String currency = Stream.concat( Stream.ofNullable( current ), all.stream( ) )
				.map( Price::currency )
				.filter( Objects::nonNull )
				.findFirst( )
				.orElse( "EUR" );
		return new PriceSchedule( current, quarters, currency );
	}

	/** KI-generiert: Viertelstunde, in die {@code instant} fällt. */
	public Optional<Price> at( Instant instant ) {
		return slots.stream( )
				.filter( p -> !p.start( ).isAfter( instant ) && p.start( ).plus( QUARTER ).isAfter( instant ) )
				.findFirst( );
	}

	/** KI-generiert: Alle Viertelstunden ab der laufenden (die laufende eingeschlossen). */
	public List<Price> from( Instant instant ) {
		return slots.stream( ).filter( p -> p.start( ).plus( QUARTER ).isAfter( instant ) ).toList( );
	}

	/** KI-generiert: Alle Viertelstunden eines Kalendertags in Berlin. */
	public List<Price> day( LocalDate date ) {
		return slots.stream( )
				.filter( p -> p.start( ).atZone( SpeechFormatter.BERLIN ).toLocalDate( ).equals( date ) )
				.toList( );
	}

	/** KI-generiert: Letzte bekannte Viertelstunde (für „Tibber hat Preise bis … veröffentlicht“). */
	public Optional<Price> lastSlot( ) {
		return slots.isEmpty( ) ? Optional.empty( ) : Optional.of( slots.getLast( ) );
	}

	/** KI-generiert: Stundenwerte = mindestens zwei Preise, alle genau eine Stunde auseinander. */
	private static boolean isHourly( List<Price> sorted ) {
		if ( sorted.size( ) < 2 ) {
			return false;
		}
		for ( int i = 1; i < sorted.size( ); i++ ) {
			if ( !Duration.between( sorted.get( i - 1 ).start( ), sorted.get( i ).start( ) ).equals( HOUR ) ) {
				return false;
			}
		}
		return true;
	}

	private static List<Price> splitHours( List<Price> hourly ) {
		List<Price> quarters = new ArrayList<>( hourly.size( ) * 4 );
		for ( Price hour : hourly ) {
			for ( int quarter = 0; quarter < 4; quarter++ ) {
				quarters.add( hour.withStart( hour.start( ).plus( QUARTER.multipliedBy( quarter ) ), SpeechFormatter.BERLIN ) );
			}
		}
		return quarters;
	}
}
