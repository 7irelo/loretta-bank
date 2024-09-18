package com.lorettabank.account.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient customerServiceRestClient(
            @Value("${customer-service.url}") String customerServiceUrl) {
        return RestClient.builder().baseUrl(customerServiceUrl).build();
    }
}
