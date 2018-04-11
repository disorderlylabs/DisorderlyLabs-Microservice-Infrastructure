package com.disorderlylabs.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;


@SpringBootApplication
@EnableHystrix
public class AppApplication {


	public static void main(String[] args) {
		SpringApplication.run(AppApplication.class, args);
	}
}
