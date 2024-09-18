package com.lorettabank.gateway.filter;

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
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);

        long startTime = System.currentTimeMillis();

        log.info(
                "Incoming request: method={}, path={}, correlationId={}",
                method,
                path,
                correlationId);

        return chain.filter(exchange)
                .then(
                        Mono.fromRunnable(
                                () -> {
                                    long duration = System.currentTimeMillis() - startTime;
                                    int statusCode =
                                            exchange.getResponse().getStatusCode() != null
                                                    ? exchange.getResponse()
                                                            .getStatusCode()
                                                            .value()
                                                    : 0;
                                    log.info(
                                            "Completed request: method={}, path={},"
                                                + " correlationId={}, status={}, duration={}ms",
                                            method,
                                            path,
                                            correlationId,
                                            statusCode,
                                            duration);
                                }));
    }

    @Override
    public int getOrder() {
        return -3;
    }
}
