package org.tholian.alexa.tibber.linking;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// KI-generiert: Fail-fast-Validierung der Account-Linking-Konfiguration (SICHERHEIT: Redirect-Allowlist)
class LinkingSettingsTest {

    private static final String LAYLA = "https://layla.amazon.com/api/skill/link/M2ABCDEFGHIJ";
    private static final String PITANGUI = "https://pitangui.amazon.com/api/skill/link/M2ABCDEFGHIJ";

    private static Map<String, String> env(String redirectUris) {
        Map<String, String> env = new HashMap<>();
        env.put("LINKING_CLIENT_ID", "tibber-skill-client");
        env.put("LINKING_REDIRECT_URIS", redirectUris);
        return env;
    }

    @Test
    void fromEnvironment_withValidValues_parsesAll() {
        var env = env(LAYLA + " , " + PITANGUI);
        env.put("PRIVACY_POLICY_URL", "https://example.org/datenschutz");

        var settings = LinkingSettings.fromEnvironment(env);

        assertThat(settings.clientId()).isEqualTo("tibber-skill-client");
        assertThat(settings.allowedRedirectUris()).containsExactlyInAnyOrder(LAYLA, PITANGUI);
        assertThat(settings.tibberApiUrl()).isEqualTo(URI.create("https://api.tibber.com/v1-beta/gql"));
        assertThat(settings.privacyPolicyUrl()).isEqualTo(URI.create("https://example.org/datenschutz"));
    }

    @Test
    void fromEnvironment_withoutPrivacyPolicy_leavesItEmpty() {
        assertThat(LinkingSettings.fromEnvironment(env(LAYLA)).privacyPolicyUrl()).isNull();
    }

    @Test
    void fromEnvironment_withoutClientId_failsFast() {
        var env = env(LAYLA);
        env.remove("LINKING_CLIENT_ID");

        assertThatThrownBy(() -> LinkingSettings.fromEnvironment(env))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("LINKING_CLIENT_ID");
    }

    @Test
    void fromEnvironment_withInvalidClientId_failsFast() {
        var env = env(LAYLA);
        env.put("LINKING_CLIENT_ID", "kurz");

        assertThatThrownBy(() -> LinkingSettings.fromEnvironment(env))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("LINKING_CLIENT_ID");
    }

    @Test
    void fromEnvironment_withoutRedirectUris_failsFast() {
        assertThatThrownBy(() -> LinkingSettings.fromEnvironment(env(" , ")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("LINKING_REDIRECT_URIS");
    }

    @Test
    void fromEnvironment_rejectsUnsafeRedirectUris() {
        for (String unsafe : new String[] {
                "http://layla.amazon.com/api/skill/link/M2ABCDEFGHIJ",
                "https://evil.example/api/skill/link/M2ABCDEFGHIJ",
                "https://layla.amazon.com.evil.example/api/skill/link/M2ABCDEFGHIJ",
                "https://evil@layla.amazon.com/api/skill/link/M2ABCDEFGHIJ",
                "https://layla.amazon.com:8443/api/skill/link/M2ABCDEFGHIJ",
                "https://layla.amazon.com/other/path",
                LAYLA + "?x=1",
                LAYLA + "#frag",
                "not a uri"}) {
            assertThatThrownBy(() -> LinkingSettings.fromEnvironment(env(unsafe)))
                    .as(unsafe)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("LINKING_REDIRECT_URIS");
        }
    }

    @Test
    void fromEnvironment_withHttpPrivacyPolicy_failsFast() {
        var env = env(LAYLA);
        env.put("PRIVACY_POLICY_URL", "http://example.org/datenschutz");

        assertThatThrownBy(() -> LinkingSettings.fromEnvironment(env))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("PRIVACY_POLICY_URL");
    }

    @Test
    void fromEnvironment_withInsecureTibberUrl_failsFast() {
        var env = env(LAYLA);
        env.put("TIBBER_API_URL", "http://api.example.com/gql");

        assertThatThrownBy(() -> LinkingSettings.fromEnvironment(env))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("TIBBER_API_URL");
    }
}
