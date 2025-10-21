package com.intellidesk.cognitia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class CognitiaApplication {

	public static void main(String[] args) {
		SpringApplication.run(CognitiaApplication.class, args);
	}

}
