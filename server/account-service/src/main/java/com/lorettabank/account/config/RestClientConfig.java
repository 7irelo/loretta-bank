package com.lorettabank.account.config;

import com.lorettabank.shared.security.InternalTokenPropagation;
import com.lorettabank.shared.security.InternalTokenService;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient customerServiceRestClient(
            @Value("${customer-service.url}") String customerServiceUrl,
            InternalTokenService tokens) {
        ClientHttpRequestFactorySettings settings =
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(Duration.ofSeconds(2))
                        .withReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
                .baseUrl(customerServiceUrl)
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .requestInterceptor(InternalTokenPropagation.restClientInterceptor(tokens))
                .build();
    }
}
