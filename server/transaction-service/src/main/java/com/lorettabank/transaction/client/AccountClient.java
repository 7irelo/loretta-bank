package com.lorettabank.transaction.client;

import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.exception.ResourceNotFoundException;
import com.lorettabank.transaction.dto.AccountBalanceResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

/**
 * Calls account-service's internal ledger API as this service (SERVICE role). A 4xx is a definitive
 * business rejection; anything else is ambiguous and surfaces as {@link LedgerUnavailableException}
 * so the caller retries with the same idempotency key.
 */
@Service
public class AccountClient {

    private final WebClient webClient;

    public AccountClient(@Qualifier("accountServiceWebClient") WebClient accountServiceWebClient) {
        this.webClient = accountServiceWebClient;
    }

    public record LedgerEntry(
            Long accountId, String systemAccount, BigDecimal amount, BigDecimal balanceAfter) {}

    public record JournalResponse(
            UUID journalId,
            String journalType,
            String reference,
            String currency,
            Instant createdAt,
            boolean replayed,
            List<LedgerEntry> entries) {}

    public AccountBalanceResponse getAccount(Long accountId) {
        return get("/internal/v1/accounts/{id}", accountId);
    }

    public AccountBalanceResponse getAccountByNumber(String accountNumber) {
        return get("/internal/v1/accounts/by-number/{number}", accountNumber);
    }

    public List<AccountBalanceResponse> getAccountsForCustomer(Long customerId) {
        try {
            return webClient
                    .get()
                    .uri("/api/v1/accounts/customer/{id}", customerId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, AccountClient::toException)
                    .bodyToMono(new ParameterizedTypeReference<List<AccountBalanceResponse>>() {})
                    .block();
        } catch (WebClientRequestException e) {
            throw new LedgerUnavailableException("Account service unavailable", e);
        }
    }

    public JournalResponse cashDeposit(
            String key,
            Long accountId,
            BigDecimal amount,
            String currency,
            String ref,
            String desc) {
        return post(
                "/internal/v1/ledger/cash-deposits",
                cashBody(key, accountId, amount, currency, ref, desc));
    }

    public JournalResponse cashWithdrawal(
            String key,
            Long accountId,
            BigDecimal amount,
            String currency,
            String ref,
            String desc) {
        return post(
                "/internal/v1/ledger/cash-withdrawals",
                cashBody(key, accountId, amount, currency, ref, desc));
    }

    public JournalResponse transfer(
            String key,
            Long sourceAccountId,
            Long targetAccountId,
            BigDecimal amount,
            String currency,
            String reference,
            String description) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("idempotencyKey", key);
        body.put("sourceAccountId", sourceAccountId);
        body.put("targetAccountId", targetAccountId);
        body.put("amount", amount);
        body.put("currency", currency);
        body.put("reference", reference);
        body.put("description", description);
        return post("/internal/v1/ledger/transfers", body);
    }

    private static Map<String, Object> cashBody(
            String key,
            Long accountId,
            BigDecimal amount,
            String currency,
            String ref,
            String desc) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("idempotencyKey", key);
        body.put("accountId", accountId);
        body.put("amount", amount);
        body.put("currency", currency);
        body.put("reference", ref);
        body.put("description", desc);
        return body;
    }

    private AccountBalanceResponse get(String uri, Object variable) {
        try {
            return webClient
                    .get()
                    .uri(uri, variable)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, AccountClient::toException)
                    .bodyToMono(AccountBalanceResponse.class)
                    .block();
        } catch (WebClientRequestException e) {
            throw new LedgerUnavailableException("Account service unavailable", e);
        }
    }

    private JournalResponse post(String uri, Map<String, Object> body) {
        try {
            return webClient
                    .post()
                    .uri(uri)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, AccountClient::toException)
                    .bodyToMono(JournalResponse.class)
                    .block();
        } catch (WebClientRequestException e) {
            throw new LedgerUnavailableException("Ledger unavailable", e);
        } catch (IllegalStateException e) {
            // block() timed out
            throw new LedgerUnavailableException("Ledger timed out", e);
        }
    }

    private static Mono<? extends Throwable> toException(
            org.springframework.web.reactive.function.client.ClientResponse response) {
        HttpStatusCode status = response.statusCode();
        return response.bodyToMono(Map.class)
                .defaultIfEmpty(Map.of())
                .map(
                        body -> {
                            Object message = body.getOrDefault("message", "Request rejected");
                            if (status.value() == HttpStatus.NOT_FOUND.value()) {
                                return new ResourceNotFoundException(String.valueOf(message));
                            }
                            if (status.is4xxClientError()) {
                                return new BusinessException(String.valueOf(message));
                            }
                            return new LedgerUnavailableException(
                                    "Ledger returned " + status.value(), null);
                        });
    }
}
