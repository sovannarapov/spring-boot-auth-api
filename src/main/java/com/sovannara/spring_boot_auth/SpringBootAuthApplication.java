package com.sovannara.spring_boot_auth;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBootAuthApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();
		dotenv.entries().forEach(
				entry -> {
					System.setProperty(entry.getKey(), entry.getValue());
					System.out.println(entry.getKey() + " = " + entry.getValue());
				}
		);

		SpringApplication.run(SpringBootAuthApplication.class, args);
	}

}
