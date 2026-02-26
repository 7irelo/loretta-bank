package com.lorettabank.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final SecretKey signingKey;
    private final List<PathPattern> openPaths;

    public JwtAuthGlobalFilter(
            @Value("${jwt.secret}") String secret,
            ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        PathPatternParser parser = new PathPatternParser();
        this.openPaths =
                List.of(
                        parser.parse("/api/v1/auth/login"),
                        parser.parse("/api/v1/auth/register"),
                        parser.parse("/api/v1/auth/refresh"),
                        parser.parse("/api/v1/{service}/v3/api-docs"),
                        parser.parse("/api/v1/{service}/swagger-ui/**"),
                        parser.parse("/actuator/**"),
                        parser.parse("/v3/api-docs/**"),
                        parser.parse("/swagger-ui/**"),
                        parser.parse("/swagger-ui.html"));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isOpenPath(request)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return onUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        Claims claims;
        try {
            claims =
                    Jwts.parser()
                            .verifyWith(signingKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return onUnauthorized(exchange, "Invalid or expired token");
        }

        String jti = claims.getId();
        if (jti == null) {
            return continueWithHeaders(exchange, chain, claims);
        }

        return redisTemplate
                .hasKey(BLACKLIST_PREFIX + jti)
                .flatMap(
                        isBlacklisted -> {
                            if (Boolean.TRUE.equals(isBlacklisted)) {
                                log.warn("Blocked blacklisted token with jti: {}", jti);
                                return onUnauthorized(exchange, "Token has been revoked");
                            }
                            return continueWithHeaders(exchange, chain, claims);
                        })
                .onErrorResume(
                        e -> {
                            log.warn(
                                    "Redis unavailable for blacklist check, allowing request: {}",
                                    e.getMessage());
                            return continueWithHeaders(exchange, chain, claims);
                        });
    }

    private Mono<Void> continueWithHeaders(
            ServerWebExchange exchange, GatewayFilterChain chain, Claims claims) {
        String userId = claims.getSubject();
        String email = claims.get("email", String.class);
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        if (roles == null || roles.isEmpty()) {
            String role = claims.get("role", String.class);
            roles = role != null ? List.of(role) : Collections.emptyList();
        }

        String roleHeader =
                roles.stream()
                        .filter(role -> role != null && !role.isBlank())
                        .map(String::trim)
                        .reduce((left, right) -> left + "," + right)
                        .orElse("");

        ServerHttpRequest mutatedRequest =
                exchange.getRequest()
                        .mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Email", email != null ? email : "")
                        .header("X-User-Roles", roleHeader)
                        .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isOpenPath(ServerHttpRequest request) {
        return openPaths.stream()
                .anyMatch(
                        pattern ->
                                pattern.matches(
                                        request.getPath().pathWithinApplication()));
    }

    private Mono<Void> onUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body =
                String.format(
                        "{\"error\":\"Unauthorized\",\"message\":\"%s\",\"status\":401}", message);
        DataBuffer buffer =
                response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
