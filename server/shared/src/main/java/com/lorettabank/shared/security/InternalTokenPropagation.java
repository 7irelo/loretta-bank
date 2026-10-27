package com.lorettabank.shared.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

/**
 * Outbound client hooks. Client code keeps describing the caller with {@code X-User-Id} and {@code
 * X-User-Roles}; these hooks replace those headers with a signed internal token before the request
 * leaves the service.
 */
public final class InternalTokenPropagation {

    private InternalTokenPropagation() {}

    public static ExchangeFilterFunction webClientFilter(InternalTokenService tokens) {
        return (request, next) ->
                next.exchange(
                        ClientRequest.from(request)
                                .headers(headers -> sign(headers, tokens))
                                .build());
    }

    public static ClientHttpRequestInterceptor restClientInterceptor(InternalTokenService tokens) {
        return (request, body, execution) -> {
            sign(request.getHeaders(), tokens);
            return execution.execute(request, body);
        };
    }

    private static void sign(HttpHeaders headers, InternalTokenService tokens) {
        String userId = headers.getFirst(JwtConstants.HEADER_USER_ID);
        String roles = headers.getFirst(JwtConstants.HEADER_USER_ROLES);
        headers.remove(JwtConstants.HEADER_USER_ID);
        headers.remove(JwtConstants.HEADER_USER_ROLES);
        headers.remove(JwtConstants.HEADER_USER_EMAIL);
        if (headers.containsKey(JwtConstants.HEADER_INTERNAL_TOKEN)) {
            return;
        }
        if (userId == null || roles == null) {
            headers.set(JwtConstants.HEADER_INTERNAL_TOKEN, tokens.mintForService("service"));
            return;
        }
        List<String> roleList =
                Arrays.stream(roles.split(","))
                        .map(String::trim)
                        .filter(r -> !r.isBlank())
                        .toList();
        headers.set(JwtConstants.HEADER_INTERNAL_TOKEN, tokens.mint(userId, roleList, null));
    }
}
