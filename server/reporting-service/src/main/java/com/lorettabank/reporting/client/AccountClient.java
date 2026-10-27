package com.lorettabank.reporting.client;

import com.lorettabank.reporting.client.dto.AccountView;
import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.security.JwtConstants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class AccountClient {

    private final WebClient accountServiceWebClient;

    public AccountClient(@Qualifier("accountServiceWebClient") WebClient accountServiceWebClient) {
        this.accountServiceWebClient = accountServiceWebClient;
    }

    public AccountView getAccount(Long accountId, Long userId, String rolesHeader) {
        return accountServiceWebClient
                .get()
                .uri("/api/v1/accounts/{id}", accountId)
                .header(JwtConstants.HEADER_USER_ID, String.valueOf(userId))
                .header(JwtConstants.HEADER_USER_ROLES, rolesHeader)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response ->
                                response.bodyToMono(String.class)
                                        .flatMap(
                                                body ->
                                                        Mono.error(
                                                                new BusinessException(
                                                                        "Failed to fetch account "
                                                                                + accountId))))
                .bodyToMono(AccountView.class)
                .block();
    }

    public java.math.BigDecimal getBalanceAt(
            Long accountId, java.time.Instant at, Long userId, String rolesHeader) {
        BalanceAt result =
                accountServiceWebClient
                        .get()
                        .uri(
                                b ->
                                        b.path("/api/v1/accounts/{id}/balance")
                                                .queryParam("at", at.toString())
                                                .build(accountId))
                        .header(JwtConstants.HEADER_USER_ID, String.valueOf(userId))
                        .header(JwtConstants.HEADER_USER_ROLES, rolesHeader)
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::isError,
                                response ->
                                        Mono.error(
                                                new BusinessException(
                                                        "Failed to fetch balance for account "
                                                                + accountId)))
                        .bodyToMono(BalanceAt.class)
                        .block();
        return result == null ? java.math.BigDecimal.ZERO : result.balance();
    }

    private record BalanceAt(java.math.BigDecimal balance) {}
}
