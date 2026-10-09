package org.tholian.alexa.tibber.linking;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

// KI-generiert: HTML-Escaping und Content-Security-Policy der Linking-Seite
class LinkingPageTest {

    @Test
    void escape_replacesHtmlMetaCharacters() {
        assertThat(LinkingPage.escape("<a href=\"x\">'&'</a>"))
                .isEqualTo("&lt;a href=&quot;x&quot;&gt;&#39;&amp;&#39;&lt;/a&gt;");
    }

    @Test
    void csp_allowsOnlyTheInlineStyleByHashAndForbidsFraming() throws Exception {
        String hash = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(LinkingPage.CSS.getBytes(StandardCharsets.UTF_8)));

        assertThat(LinkingPage.CSP)
                .startsWith("default-src 'none'")
                .contains("style-src 'sha256-" + hash + "'")
                .contains("frame-ancestors 'none'")
                .contains("base-uri 'none'")
                .contains("form-action 'self' https://alexa.amazon.co.jp https://layla.amazon.com https://pitangui.amazon.com")
                .doesNotContain("script-src");
    }
}
