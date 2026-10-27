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

    public record CustomerSummary(Long id, Long userId, String kycStatus) {
        public boolean verified() {
            return "VERIFIED".equals(kycStatus);
        }
    }

    public CustomerSummary currentCustomer(Long userId, String rolesHeader) {
        CustomerSummary summary =
                customerServiceWebClient
                        .get()
                        .uri("/api/v1/customers/me")
                        .header(JwtConstants.HEADER_USER_ID, String.valueOf(userId))
                        .header(JwtConstants.HEADER_USER_ROLES, rolesHeader)
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::isError,
                                response ->
                                        Mono.error(
                                                new BusinessException(
                                                        "Complete your customer profile first")))
                        .bodyToMono(CustomerSummary.class)
                        .block();

        if (summary == null || summary.id() == null) {
            throw new BusinessException("Could not resolve current customer profile");
        }
        return summary;
    }

    public Long resolveCurrentCustomerId(Long userId, String rolesHeader) {
        return currentCustomer(userId, rolesHeader).id();
    }
}
