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
}
