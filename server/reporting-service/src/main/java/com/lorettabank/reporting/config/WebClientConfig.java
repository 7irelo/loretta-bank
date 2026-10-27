package com.lorettabank.reporting.config;

import com.lorettabank.shared.security.InternalTokenPropagation;
import com.lorettabank.shared.security.InternalTokenService;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient accountServiceWebClient(
            @Value("${account-service.url}") String accountServiceUrl,
            InternalTokenService tokens) {
        return build(accountServiceUrl, tokens);
    }

    @Bean
    public WebClient transactionServiceWebClient(
            @Value("${transaction-service.url}") String transactionServiceUrl,
            InternalTokenService tokens) {
        return build(transactionServiceUrl, tokens);
    }

    @Bean
    public WebClient customerServiceWebClient(
            @Value("${customer-service.url}") String customerServiceUrl,
            InternalTokenService tokens) {
        return build(customerServiceUrl, tokens);
    }

    static WebClient build(String baseUrl, InternalTokenService tokens) {
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(5));
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(InternalTokenPropagation.webClientFilter(tokens))
                .build();
    }
}
