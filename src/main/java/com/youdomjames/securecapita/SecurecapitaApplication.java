package com.youdomjames.securecapita;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collections;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class SecurecapitaApplication {
	private static final int STRENGTH = 12;
	public static void main(String[] args) {
		SpringApplication.run(SecurecapitaApplication.class, args);
	}
	@Bean
	public BCryptPasswordEncoder passwordEncoder(){
		return new BCryptPasswordEncoder(STRENGTH);
	}
}

