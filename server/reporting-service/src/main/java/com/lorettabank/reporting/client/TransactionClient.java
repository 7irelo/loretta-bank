package com.lorettabank.reporting.client;

import com.lorettabank.reporting.client.dto.PagedResponseView;
import com.lorettabank.reporting.client.dto.TransactionView;
import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.security.JwtConstants;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class TransactionClient {

    private static final int PAGE_SIZE = 200;
    private static final int MAX_PAGES = 50;

    private final WebClient transactionServiceWebClient;

    public TransactionClient(
            @Qualifier("transactionServiceWebClient") WebClient transactionServiceWebClient) {
        this.transactionServiceWebClient = transactionServiceWebClient;
    }

    public List<TransactionView> getAllTransactionsForAccount(
            Long accountId, Long userId, String rolesHeader) {
        int page = 0;
        boolean last = false;
        List<TransactionView> aggregated = new java.util.ArrayList<>();

        while (!last && page < MAX_PAGES) {
            PagedResponseView<TransactionView> response =
                    fetchPage(accountId, page, PAGE_SIZE, userId, rolesHeader);
            if (response.getContent() != null) {
                aggregated.addAll(response.getContent());
            }
            last = response.isLast();
            page++;
        }

        return Collections.unmodifiableList(aggregated);
    }

    private PagedResponseView<TransactionView> fetchPage(
            Long accountId, int page, int size, Long userId, String rolesHeader) {
        PagedResponseView<TransactionView> response =
                transactionServiceWebClient.get()
                        .uri(
                                uriBuilder ->
                                        uriBuilder.path("/api/v1/transactions/account/{accountId}")
                                                .queryParam("page", page)
                                                .queryParam("size", size)
                                                .build(accountId))
                        .header(JwtConstants.HEADER_USER_ID, String.valueOf(userId))
                        .header(JwtConstants.HEADER_USER_ROLES, rolesHeader)
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::isError,
                                httpResponse ->
                                        httpResponse
                                                .bodyToMono(String.class)
                                                .flatMap(
                                                        body ->
                                                                Mono.error(
                                                                        new BusinessException(
                                                                                "Failed to fetch transactions for account "
                                                                                        + accountId
                                                                                        + ": "
                                                                                        + body))))
                        .bodyToMono(
                                new ParameterizedTypeReference<
                                        PagedResponseView<TransactionView>>() {})
                        .block();

        return response != null
                ? response
                : PagedResponseView.<TransactionView>builder()
                        .content(Collections.emptyList())
                        .last(true)
                        .build();
    }
}
