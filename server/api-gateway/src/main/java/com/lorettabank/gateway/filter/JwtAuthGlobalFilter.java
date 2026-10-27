package com.lorettabank.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
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
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String INTERNAL_AUDIENCE = "loretta-internal";
    private static final Duration INTERNAL_TOKEN_TTL = Duration.ofSeconds(60);
    private static final List<String> SPOOFABLE_HEADERS =
            List.of("X-User-Id", "X-User-Email", "X-User-Roles", INTERNAL_TOKEN_HEADER);

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final SecretKey signingKey;
    private final SecretKey internalKey;
    private final List<PathPattern> openPaths;

    public JwtAuthGlobalFilter(
            @Value("${jwt.secret}") String secret,
            @Value("${internal.jwt.secret}") String internalSecret,
            ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.internalKey = Keys.hmacShaKeyFor(internalSecret.getBytes(StandardCharsets.UTF_8));

        PathPatternParser parser = new PathPatternParser();
        this.openPaths =
                List.of(
                        parser.parse("/api/v1/auth/login"),
                        parser.parse("/api/v1/auth/register"),
                        parser.parse("/api/v1/auth/refresh"),
                        parser.parse("/api/v1/{service}/v3/api-docs"),
                        parser.parse("/api/v1/{service}/swagger-ui/**"),
                        parser.parse("/actuator/health/**"),
                        parser.parse("/v3/api-docs/**"),
                        parser.parse("/swagger-ui/**"),
                        parser.parse("/swagger-ui.html"));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Never let a client supply identity headers; only this filter may set them.
        ServerHttpRequest request =
                exchange.getRequest()
                        .mutate()
                        .headers(headers -> SPOOFABLE_HEADERS.forEach(headers::remove))
                        .build();
        exchange = exchange.mutate().request(request).build();

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

        if (!"access".equals(claims.get("type", String.class))) {
            return onUnauthorized(exchange, "Access token required");
        }

        String jti = claims.getId();
        if (jti == null) {
            return onUnauthorized(exchange, "Token has no identifier");
        }

        ServerWebExchange current = exchange;

        return redisTemplate
                .hasKey(BLACKLIST_PREFIX + jti)
                .flatMap(
                        isBlacklisted -> {
                            if (Boolean.TRUE.equals(isBlacklisted)) {
                                log.warn("Blocked blacklisted token with jti: {}", jti);
                                return onUnauthorized(current, "Token has been revoked");
                            }
                            return continueWithHeaders(current, chain, claims);
                        })
                .onErrorResume(
                        e -> {
                            // Fail closed: without the revocation list we cannot tell whether
                            // this token was logged out or reported stolen.
                            log.error("Redis unavailable for revocation check: {}", e.getMessage());
                            return onStatus(
                                    current,
                                    HttpStatus.SERVICE_UNAVAILABLE,
                                    "Authentication temporarily unavailable");
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
                        .header(INTERNAL_TOKEN_HEADER, mintInternalToken(userId, roles, email))
                        .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private String mintInternalToken(String userId, List<String> roles, String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .audience()
                .add(INTERNAL_AUDIENCE)
                .and()
                .claim("roles", roles)
                .claim("email", email != null ? email : "")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + INTERNAL_TOKEN_TTL.toMillis()))
                .signWith(internalKey)
                .compact();
    }

    private boolean isOpenPath(ServerHttpRequest request) {
        return openPaths.stream()
                .anyMatch(pattern -> pattern.matches(request.getPath().pathWithinApplication()));
    }

    private Mono<Void> onUnauthorized(ServerWebExchange exchange, String message) {
        return onStatus(exchange, HttpStatus.UNAUTHORIZED, message);
    }

    private Mono<Void> onStatus(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body =
                String.format(
                        "{\"error\":\"%s\",\"message\":\"%s\",\"status\":%d}",
                        status.getReasonPhrase(), message, status.value());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
