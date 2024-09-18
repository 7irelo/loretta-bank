package com.lorettabank.reporting.client;

import com.lorettabank.reporting.client.dto.CustomerView;
import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.security.JwtConstants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CustomerClient {

    private final WebClient customerServiceWebClient;

    public CustomerClient(
            @Qualifier("customerServiceWebClient") WebClient customerServiceWebClient) {
        this.customerServiceWebClient = customerServiceWebClient;
    }

    public CustomerView getCurrentCustomer(Long userId, String rolesHeader) {
        return customerServiceWebClient.get()
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
                .bodyToMono(CustomerView.class)
                .block();
    }
}
