package com.intellidesk.cognitia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class CognitiaApplication {

	public static void main(String[] args) {
		SpringApplication.run(CognitiaApplication.class, args);
	}

	@Bean
	public ObjectMapper objectMapper(){
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}

}
