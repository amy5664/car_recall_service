package com.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class BootCarRecallApplication {

	public static void main(String[] args) {
		SpringApplication.run(BootCarRecallApplication.class, args);
	}
}