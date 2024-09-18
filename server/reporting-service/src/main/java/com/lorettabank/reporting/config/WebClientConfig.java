package com.lorettabank.reporting.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient accountServiceWebClient(
            @Value("${account-service.url}") String accountServiceUrl) {
        return WebClient.builder().baseUrl(accountServiceUrl).build();
    }

    @Bean
    public WebClient transactionServiceWebClient(
            @Value("${transaction-service.url}") String transactionServiceUrl) {
        return WebClient.builder().baseUrl(transactionServiceUrl).build();
    }

    @Bean
    public WebClient customerServiceWebClient(
            @Value("${customer-service.url}") String customerServiceUrl) {
        return WebClient.builder().baseUrl(customerServiceUrl).build();
    }
}
