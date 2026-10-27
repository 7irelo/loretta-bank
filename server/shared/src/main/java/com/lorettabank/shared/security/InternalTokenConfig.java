package com.lorettabank.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalTokenConfig {

    @Bean
    public InternalTokenService internalTokenService(
            @Value("${internal.jwt.secret:${INTERNAL_JWT_SECRET:}}") String secret) {
        return new InternalTokenService(secret);
    }
}
