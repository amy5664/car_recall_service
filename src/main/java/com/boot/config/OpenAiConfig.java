package com.boot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OpenAiConfig {
	  @Value("${openai.api-key}")
	    private String apiKey;
	 
	    @Bean
	    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
	        return restTemplateBuilder
	                .additionalInterceptors(((request, body, execution) -> {
	                    request.getHeaders().set("Authorization", "Bearer " + apiKey);
	                    request.getHeaders().set("Content-Type", "application/json");
	                    return execution.execute(request, body);
	                }))
	                .build();
	    }
}
