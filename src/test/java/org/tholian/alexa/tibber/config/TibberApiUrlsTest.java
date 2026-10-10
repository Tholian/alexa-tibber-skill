package org.tholian.alexa.tibber.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KI-generiert: SICHERHEIT – über die WebSocket-URL wird der Token gesendet. Sie stammt aus der API-Antwort
 * und darf nur auf Tibber (wss) oder – für Tests – auf Loopback (ws) zeigen.
 */
class TibberApiUrlsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "wss://websocket-api.tibber.com/v1-beta/gql/subscriptions",
            "wss://tibber.com/x",
            "WSS://Websocket-API.Tibber.com/x",
            "ws://127.0.0.1:4711/x",
            "ws://localhost/x"
    })
    void webSocketUrl_acceptsTibberAndLoopback(String url) {
        assertThat(TibberApiUrls.webSocketUrl(url)).isPresent();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "ws://websocket-api.tibber.com/x",
            "wss://evil.example.com/x",
            "wss://websocket-api.tibber.com.evil.example/x",
            "wss://eviltibber.com/x",
            "wss://user@websocket-api.tibber.com/x",
            "https://websocket-api.tibber.com/x",
            "ws://192.168.1.10/x",
            "kein uri"
    })
    void webSocketUrl_rejectsEverythingElse(String url) {
        assertThat(TibberApiUrls.webSocketUrl(url)).isEmpty();
    }
}
