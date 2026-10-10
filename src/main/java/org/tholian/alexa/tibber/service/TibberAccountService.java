package org.tholian.alexa.tibber.service;

import org.intellij.lang.annotations.Language;

/**
 * KI-generiert: Prüft beim Account Linking, ob Tibber einen Token akzeptiert und mindestens ein Zuhause liefert.
 * Datensparsamkeit: Die Abfrage liest nur Home-IDs – keine Preise, Verbräuche oder personenbezogenen Felder.
 */
public class TibberAccountService {

	@Language( "GraphQL" )
	private static final String VERIFY_QUERY = """
			{
			  viewer {
			    homes { id }
			  }
			}
			""";

	private final TibberGraphQlClient client;

	public TibberAccountService( TibberGraphQlClient client ) {
		this.client = client;
	}

	/**
	 * @throws TibberAuthenticationException wenn Tibber den Token ablehnt
	 * @throws TibberApiException            bei sonstigen Fehlern oder wenn kein Zuhause existiert
	 */
	public void verifyToken( String token ) {
		TibberGraphQlClient.firstHome( client.query( token, VERIFY_QUERY ) );
	}
}
