package org.tholian.alexa.tibber.config;

import com.amazon.ask.CustomSkill;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// KI-generiert: Übergangslösung bis Task 3 – Spring delegiert vollständig an SkillFactory; Datei wird dort gelöscht
@Configuration
public class SkillConfig {

	@Bean
	public CustomSkill skill( ) {
		return SkillFactory.create( SkillSettings.fromEnvironment( System.getenv( ) ) );
	}
}
