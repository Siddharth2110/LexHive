package com.example.LexHive.booklender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LexHiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(LexHiveApplication.class, args);
	}

}
