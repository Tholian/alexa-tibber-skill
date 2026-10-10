package org.tholian.alexa.tibber.config;

import java.net.URI;
import java.time.Clock;

import org.tholian.alexa.tibber.service.EnergyService;
import org.tholian.alexa.tibber.service.LiveMeasurementClient;
import org.tholian.alexa.tibber.service.LiveService;
import org.tholian.alexa.tibber.service.PriceService;
import org.tholian.alexa.tibber.service.SpeechFormatter;
import org.tholian.alexa.tibber.service.TibberGraphQlClient;

/**
 * KI-generiert: Alle Services des Skills – einmal pro Lambda Execution Environment gebaut und an die Handler
 * verteilt. HTTP und WebSocket nutzen denselben JDK-HttpClient; eine gemeinsame Uhr sorgt für ein einheitliches „jetzt“.
 */
public record SkillServices( PriceService prices, EnergyService energy, LiveService live, SpeechFormatter formatter, Clock clock ) {

	public static SkillServices create( URI tibberApiUrl, Clock clock ) {
		TibberGraphQlClient client = SkillFactory.tibberClient( tibberApiUrl );
		LiveMeasurementClient liveClient = new LiveMeasurementClient( client.httpClient( ), client.objectMapper( ) );
		return new SkillServices( new PriceService( client ), new EnergyService( client, clock ),
				new LiveService( client, liveClient ), new SpeechFormatter( clock ), clock );
	}
}
