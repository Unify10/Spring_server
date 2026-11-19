package com.unify.onyak;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class OnyakApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnyakApplication.class, args);
	}

}
