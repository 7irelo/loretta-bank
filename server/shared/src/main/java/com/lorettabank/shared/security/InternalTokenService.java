package com.lorettabank.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;

/**
 * Mints and verifies short-lived internal identity tokens. Services trust only these tokens, never
 * raw identity headers, so a caller that can reach a service directly cannot claim to be someone
 * else without the internal signing key.
 */
public class InternalTokenService {

    public static final String AUDIENCE = "loretta-internal";
    private static final Duration TTL = Duration.ofSeconds(60);

    private final SecretKey key;

    public InternalTokenService(String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "INTERNAL_JWT_SECRET must be set and at least 32 characters long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String mint(String subject, List<String> roles, String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .audience()
                .add(AUDIENCE)
                .and()
                .claim("roles", roles)
                .claim("email", email == null ? "" : email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + TTL.toMillis()))
                .signWith(key)
                .compact();
    }

    public String mintForService(String serviceName) {
        return mint(
                JwtConstants.SYSTEM_USER_ID,
                List.of(JwtConstants.ROLE_SERVICE),
                serviceName + "@internal");
    }

    public Claims verify(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireAudience(AUDIENCE)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Invalid internal token", e);
        }
    }
}
