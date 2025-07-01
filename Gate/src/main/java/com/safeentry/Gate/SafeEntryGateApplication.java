package com.safeentry.Gate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka; // Adicione este import

@SpringBootApplication
@EnableKafka // Adicione esta anotação
public class SafeEntryGateApplication {

	public static void main(String[] args) {
		SpringApplication.run(SafeEntryGateApplication.class, args);
	}

}