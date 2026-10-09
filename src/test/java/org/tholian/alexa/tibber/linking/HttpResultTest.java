package org.tholian.alexa.tibber.linking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KI-generiert: Tests für HttpResult – insbesondere, dass toString den Token im Location-Header nicht preisgibt.
 */
class HttpResultTest {

    @Test
    void toString_redactsLocationHeader() {
        String text = HttpResult.redirect("https://x#access_token=SECRET").toString();

        assertThat(text).doesNotContain("SECRET").contains("Location=<redacted>").contains("302");
    }

    @Test
    void toString_keepsOtherHeadersAndStatus() {
        String text = HttpResult.error(404, "Seite nicht gefunden.").toString();

        assertThat(text).contains("404").contains("Cache-Control=no-store");
    }
}
