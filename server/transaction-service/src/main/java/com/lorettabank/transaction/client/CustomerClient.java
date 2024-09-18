package com.lorettabank.transaction.client;

import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.security.JwtConstants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class CustomerClient {

    private final WebClient customerServiceWebClient;

    public CustomerClient(
            @Qualifier("customerServiceWebClient") WebClient customerServiceWebClient) {
        this.customerServiceWebClient = customerServiceWebClient;
    }

    public Long resolveCurrentCustomerId(Long userId, String rolesHeader) {
        CustomerSummary summary =
                customerServiceWebClient.get()
                        .uri("/api/v1/customers/me")
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
                                                                                "Failed to resolve current customer: "
                                                                                        + body))))
                        .bodyToMono(CustomerSummary.class)
                        .block();

        if (summary == null || summary.getId() == null) {
            throw new BusinessException("Could not resolve current customer profile");
        }

        return summary.getId();
    }

    private static class CustomerSummary {
        private Long id;
        private Long userId;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }
}
