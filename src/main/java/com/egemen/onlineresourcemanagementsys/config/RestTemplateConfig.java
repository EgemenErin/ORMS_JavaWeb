package com.egemen.onlineresourcemanagementsys.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for defining beans such as RestTemplate.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Bean for RestTemplate to enable HTTP calls to external services.
     *
     * @return RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
