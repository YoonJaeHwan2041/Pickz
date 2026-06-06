package com.yoon.pickz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class PickzApplication {

	public static void main(String[] args) {
		SpringApplication.run(PickzApplication.class, args);
	}

}
