package com.vigneshsuryah.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class })
@EnableSwagger2
@ComponentScan(basePackages = "com.vigneshsuryah.springboot")
public class Application extends SpringBootServletInitializer {

	
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);

	}

}
