package com.lorettabank.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests from the signed {@code X-Internal-Token} minted by the gateway or by a
 * peer service. Raw {@code X-User-*} headers are ignored.
 */
public class GatewayAuthFilter extends OncePerRequestFilter {

    private final InternalTokenService tokenService;

    public GatewayAuthFilter(InternalTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader(JwtConstants.HEADER_INTERNAL_TOKEN);

        if (token != null && !token.isBlank()) {
            try {
                Claims claims = tokenService.verify(token);
                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);
                List<SimpleGrantedAuthority> authorities =
                        (roles == null ? List.<String>of() : roles)
                                .stream()
                                        .map(String::trim)
                                        .filter(role -> !role.isBlank())
                                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                        .toList();
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                claims.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
