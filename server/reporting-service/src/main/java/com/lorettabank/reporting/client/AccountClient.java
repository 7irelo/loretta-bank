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
        return accountServiceWebClient.get()
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
                                                                                + accountId
                                                                                + ": "
                                                                                + body))))
                .bodyToMono(AccountView.class)
                .block();
    }
}
