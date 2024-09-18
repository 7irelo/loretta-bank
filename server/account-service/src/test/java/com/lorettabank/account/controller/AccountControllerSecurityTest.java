package com.lorettabank.account.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lorettabank.account.client.CustomerIdentityClient;
import com.lorettabank.account.dto.AccountResponse;
import com.lorettabank.account.entity.AccountStatus;
import com.lorettabank.account.entity.AccountType;
import com.lorettabank.account.service.AccountService;
import com.lorettabank.shared.security.ServiceSecurityConfig;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
@Import(ServiceSecurityConfig.class)
class AccountControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private CustomerIdentityClient customerIdentityClient;

    @Test
    void customerCannotReadAnotherCustomersAccount() throws Exception {
        when(accountService.getAccount(1L)).thenReturn(buildAccountResponse(2L));
        when(customerIdentityClient.resolveCurrentCustomerId(1L, "CUSTOMER")).thenReturn(1L);

        mockMvc.perform(
                        get("/api/v1/accounts/1")
                                .header("X-User-Id", "1")
                                .header("X-User-Roles", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadAnyAccount() throws Exception {
        when(accountService.getAccount(1L)).thenReturn(buildAccountResponse(2L));

        mockMvc.perform(
                        get("/api/v1/accounts/1")
                                .header("X-User-Id", "1")
                                .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk());
    }

    private AccountResponse buildAccountResponse(Long customerId) {
        return AccountResponse.builder()
                .id(1L)
                .accountNumber("LOR0000000001")
                .customerId(customerId)
                .accountType(AccountType.CHECKING)
                .currency("ZAR")
                .balance(new BigDecimal("1000.0000"))
                .overdraftEnabled(false)
                .overdraftLimit(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();
    }
}
