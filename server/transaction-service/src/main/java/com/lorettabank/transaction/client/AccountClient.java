package com.lorettabank.transaction.client;

import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.transaction.dto.AccountBalanceResponse;
import java.math.BigDecimal;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AccountClient {

    private static final Logger log = LoggerFactory.getLogger(AccountClient.class);

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLES = "X-User-Roles";
    private static final String SYSTEM_USER_ID = "0";
    private static final String SYSTEM_ROLE = "ADMIN";

    private final WebClient webClient;

    public AccountClient(@Qualifier("accountServiceWebClient") WebClient accountServiceWebClient) {
        this.webClient = accountServiceWebClient;
    }

    public AccountBalanceResponse getAccount(Long accountId) {
        log.debug("Fetching account details for accountId={}", accountId);
        return webClient.get()
                .uri("/api/v1/accounts/{id}", accountId)
                .header(HEADER_USER_ID, SYSTEM_USER_ID)
                .header(HEADER_USER_ROLES, SYSTEM_ROLE)
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
                .bodyToMono(AccountBalanceResponse.class)
                .block();
    }

    public void debit(Long accountId, BigDecimal amount, String reference) {
        log.debug(
                "Debiting accountId={}, amount={}, reference={}",
                accountId,
                amount,
                reference);
        webClient
                .post()
                .uri("/api/v1/accounts/{id}/withdraw", accountId)
                .header(HEADER_USER_ID, SYSTEM_USER_ID)
                .header(HEADER_USER_ROLES, SYSTEM_ROLE)
                .bodyValue(Map.of("amount", amount, "reference", reference))
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response ->
                                response.bodyToMono(String.class)
                                        .flatMap(
                                                body ->
                                                        Mono.error(
                                                                new BusinessException(
                                                                        "Failed to debit account "
                                                                                + accountId
                                                                                + ": "
                                                                                + body))))
                .toBodilessEntity()
                .block();
    }

    public void credit(Long accountId, BigDecimal amount, String reference) {
        log.debug(
                "Crediting accountId={}, amount={}, reference={}",
                accountId,
                amount,
                reference);
        webClient
                .post()
                .uri("/api/v1/accounts/{id}/deposit", accountId)
                .header(HEADER_USER_ID, SYSTEM_USER_ID)
                .header(HEADER_USER_ROLES, SYSTEM_ROLE)
                .bodyValue(Map.of("amount", amount, "reference", reference))
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response ->
                                response.bodyToMono(String.class)
                                        .flatMap(
                                                body ->
                                                        Mono.error(
                                                                new BusinessException(
                                                                        "Failed to credit account "
                                                                                + accountId
                                                                                + ": "
                                                                                + body))))
                .toBodilessEntity()
                .block();
    }
}
