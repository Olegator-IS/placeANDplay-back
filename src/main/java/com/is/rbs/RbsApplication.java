package com.is.rbs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class RbsApplication extends SpringBootServletInitializer{

	public static void main(String[] args) {
		SpringApplication.run(RbsApplication.class, args);
	}
	
	
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(RbsApplication.class);
    }

}
