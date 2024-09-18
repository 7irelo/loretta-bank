package com.lorettabank.account.client;

import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.security.JwtConstants;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CustomerIdentityClient {

    private final RestClient customerServiceRestClient;

    public CustomerIdentityClient(RestClient customerServiceRestClient) {
        this.customerServiceRestClient = customerServiceRestClient;
    }

    public Long resolveCurrentCustomerId(Long userId, String rolesHeader) {
        try {
            CustomerSummary response =
                    customerServiceRestClient.get()
                            .uri("/api/v1/customers/me")
                            .header(JwtConstants.HEADER_USER_ID, String.valueOf(userId))
                            .header(JwtConstants.HEADER_USER_ROLES, rolesHeader)
                            .retrieve()
                            .body(CustomerSummary.class);
            if (response == null || response.id() == null) {
                throw new BusinessException("Could not resolve current customer profile");
            }
            return response.id();
        } catch (RestClientException e) {
            throw new BusinessException(
                    "Failed to resolve current customer profile: " + e.getMessage());
        }
    }

    private record CustomerSummary(Long id, Long userId) {}
}
