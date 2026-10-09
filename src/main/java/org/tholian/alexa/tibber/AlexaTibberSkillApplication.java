package org.tholian.alexa.tibber;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class AlexaTibberSkillApplication {

	public static void main( String[] args ) {
		SpringApplication.run( AlexaTibberSkillApplication.class, args );
		log.info( "Application started" );
	}
}
