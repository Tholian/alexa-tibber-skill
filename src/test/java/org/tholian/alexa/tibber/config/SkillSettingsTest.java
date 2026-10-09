package org.tholian.alexa.tibber.config;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// KI-generiert: Konfiguration aus Umgebungsvariablen – fail-fast ohne Skill-ID (SICHERHEIT)
class SkillSettingsTest {

    @Test
    void fromEnvironment_withSkillIdOnly_usesDefaultTibberUrl() {
        var settings = SkillSettings.fromEnvironment(Map.of("ALEXA_SKILL_ID", "amzn1.ask.skill.abc"));

        assertThat(settings.skillId()).isEqualTo("amzn1.ask.skill.abc");
        assertThat(settings.tibberApiUrl()).isEqualTo(URI.create("https://api.tibber.com/v1-beta/gql"));
    }

    @Test
    void fromEnvironment_withCustomTibberUrl_usesIt() {
        var settings = SkillSettings.fromEnvironment(Map.of(
                "ALEXA_SKILL_ID", "amzn1.ask.skill.abc",
                "TIBBER_API_URL", "http://127.0.0.1:9999/gql"));

        assertThat(settings.tibberApiUrl()).isEqualTo(URI.create("http://127.0.0.1:9999/gql"));
    }

    @Test
    void fromEnvironment_stripsSkillId() {
        var settings = SkillSettings.fromEnvironment(Map.of("ALEXA_SKILL_ID", "  amzn1.ask.skill.abc \n"));

        assertThat(settings.skillId()).isEqualTo("amzn1.ask.skill.abc");
    }

    @Test
    void fromEnvironment_withoutSkillId_failsFast() {
        assertThatThrownBy(() -> SkillSettings.fromEnvironment(Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ALEXA_SKILL_ID");
    }

    @Test
    void fromEnvironment_withBlankSkillId_failsFast() {
        assertThatThrownBy(() -> SkillSettings.fromEnvironment(Map.of("ALEXA_SKILL_ID", "   ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ALEXA_SKILL_ID");
    }

    @Test
    void fromEnvironment_withHttpNonLoopbackUrl_failsFast() {
        assertThatThrownBy(() -> SkillSettings.fromEnvironment(Map.of(
                "ALEXA_SKILL_ID", "amzn1.ask.skill.abc",
                "TIBBER_API_URL", "http://api.example.com/gql")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TIBBER_API_URL");
    }

    @Test
    void fromEnvironment_withHttpsUrl_isAccepted() {
        var settings = SkillSettings.fromEnvironment(Map.of(
                "ALEXA_SKILL_ID", "amzn1.ask.skill.abc",
                "TIBBER_API_URL", "https://api.example.com/gql"));

        assertThat(settings.tibberApiUrl()).isEqualTo(URI.create("https://api.example.com/gql"));
    }

    @Test
    void fromEnvironment_withHttpIpv6Loopback_isAccepted() {
        var settings = SkillSettings.fromEnvironment(Map.of(
                "ALEXA_SKILL_ID", "amzn1.ask.skill.abc",
                "TIBBER_API_URL", "http://[::1]:9999/gql"));

        assertThat(settings.tibberApiUrl().getPort()).isEqualTo(9999);
    }

    @Test
    void fromEnvironment_withInvalidUrl_failsFast() {
        assertThatThrownBy(() -> SkillSettings.fromEnvironment(Map.of(
                "ALEXA_SKILL_ID", "amzn1.ask.skill.abc",
                "TIBBER_API_URL", "not a url")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TIBBER_API_URL");
    }

    @Test
    void fromEnvironment_withRelativeUrl_failsFast() {
        assertThatThrownBy(() -> SkillSettings.fromEnvironment(Map.of(
                "ALEXA_SKILL_ID", "amzn1.ask.skill.abc",
                "TIBBER_API_URL", "/gql")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TIBBER_API_URL");
    }
}
