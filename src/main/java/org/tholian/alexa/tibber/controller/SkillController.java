package org.tholian.alexa.tibber.controller;

import com.amazon.ask.CustomSkill;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.ResponseEnvelope;
import com.amazon.ask.util.impl.ObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class SkillController {

	private static final Logger log = LoggerFactory.getLogger( SkillController.class );

	private final CustomSkill skill;
	private final ObjectMapper objectMapper;

	public SkillController( CustomSkill skill ) {
		this.skill = skill;
		this.objectMapper = ObjectMapperFactory.getMapper( );
	}

	@PostMapping( value = "/alexa", produces = MediaType.APPLICATION_JSON_VALUE )
	public String handleAlexaRequest( @RequestBody String body ) throws Exception {
		RequestEnvelope requestEnvelope = objectMapper.readValue( body, RequestEnvelope.class );
		ResponseEnvelope responseEnvelope = skill.invoke( requestEnvelope, null );
		return objectMapper.writeValueAsString( responseEnvelope );
	}
}
