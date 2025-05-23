package com.is;

import com.is.auth.config.SSHTunnel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class RbsApplication extends SpringBootServletInitializer{

	public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent"));
        SSHTunnel.createSSHTunnel(); // Устанавливаем SSH туннель
		SpringApplication.run(RbsApplication.class, args);
	}
	
	
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(RbsApplication.class);
    }

}
