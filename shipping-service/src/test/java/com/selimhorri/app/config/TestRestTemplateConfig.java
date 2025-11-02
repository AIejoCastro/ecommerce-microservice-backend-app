package com.selimhorri.app.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
public class TestRestTemplateConfig {

    @Bean(name = "restTemplateBean")
    @Primary
    RestTemplate restTemplateBean() {
        return new RestTemplate();
    }
}
