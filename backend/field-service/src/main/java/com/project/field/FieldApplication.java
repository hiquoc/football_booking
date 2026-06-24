package com.project.field;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.project.field", "com.project.common"})
@EnableFeignClients
public class FieldApplication {

	public static void main(String[] args) {
		SpringApplication.run(FieldApplication.class, args);
	}

}
