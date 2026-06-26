package com.project.field;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.project.field", "com.project.common"})
@AutoConfigurationPackage(basePackages = {"com.project.field", "com.project.common"})
@EnableFeignClients
@EnableScheduling
public class FieldApplication {

	public static void main(String[] args) {
		SpringApplication.run(FieldApplication.class, args);
	}

}
