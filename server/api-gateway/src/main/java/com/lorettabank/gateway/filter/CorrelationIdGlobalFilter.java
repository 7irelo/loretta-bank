package com.lorettabank.gateway.filter;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId =
                exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        ServerHttpRequest mutatedRequest =
                exchange.getRequest()
                        .mutate()
                        .header(CORRELATION_ID_HEADER, correlationId)
                        .build();

        String finalCorrelationId = correlationId;

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .then(
                        Mono.fromRunnable(
                                () ->
                                        exchange.getResponse()
                                                .getHeaders()
                                                .add(
                                                        CORRELATION_ID_HEADER,
                                                        finalCorrelationId)));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
