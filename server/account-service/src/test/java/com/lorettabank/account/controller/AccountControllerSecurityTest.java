package com.lorettabank.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lorettabank.account.client.CustomerIdentityClient;
import com.lorettabank.account.client.CustomerIdentityClient.CustomerSummary;
import com.lorettabank.account.dto.AccountResponse;
import com.lorettabank.account.entity.AccountStatus;
import com.lorettabank.account.entity.AccountType;
import com.lorettabank.account.ledger.LedgerService;
import com.lorettabank.account.service.AccountService;
import com.lorettabank.shared.exception.GlobalExceptionHandler;
import com.lorettabank.shared.security.InternalTokenConfig;
import com.lorettabank.shared.security.InternalTokenService;
import com.lorettabank.shared.security.ServiceSecurityConfig;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AccountController.class, InternalLedgerController.class})
@Import({ServiceSecurityConfig.class, InternalTokenConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(
        properties = "internal.jwt.secret=test-internal-secret-that-is-long-enough-0123456789")
class AccountControllerSecurityTest {

    private static final String TOKEN = "X-Internal-Token";

    @Autowired private MockMvc mockMvc;
    @Autowired private InternalTokenService tokens;

    @MockBean private AccountService accountService;
    @MockBean private LedgerService ledgerService;
    @MockBean private CustomerIdentityClient customerIdentityClient;

    @Test
    void rawIdentityHeadersAreNotTrusted() throws Exception {
        when(accountService.getAccount(1L)).thenReturn(account(2L));

        mockMvc.perform(
                        get("/api/v1/accounts/1")
                                .header("X-User-Id", "1")
                                .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    void forgedInternalTokenIsRejected() throws Exception {
        String forged =
                new InternalTokenService("an-attacker-controlled-secret-0000000000000")
                        .mint("1", List.of("ADMIN"), null);
        mockMvc.perform(get("/api/v1/accounts/1").header(TOKEN, forged))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotReadAnotherCustomersAccount() throws Exception {
        when(accountService.getAccount(1L)).thenReturn(account(2L));
        when(customerIdentityClient.currentCustomer(1L, "CUSTOMER"))
                .thenReturn(new CustomerSummary(1L, 1L, "VERIFIED"));

        mockMvc.perform(get("/api/v1/accounts/1").header(TOKEN, as("1", "CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadAnyAccount() throws Exception {
        when(accountService.getAccount(1L)).thenReturn(account(2L));

        mockMvc.perform(get("/api/v1/accounts/1").header(TOKEN, as("9", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void unverifiedCustomerCannotOpenAccount() throws Exception {
        when(customerIdentityClient.currentCustomer(1L, "CUSTOMER"))
                .thenReturn(new CustomerSummary(1L, 1L, "PENDING"));

        mockMvc.perform(
                        post("/api/v1/accounts")
                                .header(TOKEN, as("1", "CUSTOMER"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"customerId\":1,\"accountType\":\"SAVINGS\"}"))
                .andExpect(status().isForbidden());
        verify(accountService, never()).createAccount(any());
    }

    @Test
    void internalLedgerRejectsEvenAdmins() throws Exception {
        mockMvc.perform(
                        post("/internal/v1/ledger/cash-deposits")
                                .header(TOKEN, as("9", "ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"idempotencyKey\":\"k1\",\"accountId\":1,"
                                                + "\"amount\":10,\"currency\":\"ZAR\","
                                                + "\"reference\":\"r\"}"))
                .andExpect(status().isForbidden());
        verify(ledgerService, never()).cashDeposit(any(), any());
    }

    @Test
    void internalLedgerAcceptsServices() throws Exception {
        mockMvc.perform(
                        post("/internal/v1/ledger/cash-deposits")
                                .header(TOKEN, tokens.mintForService("transaction-service"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"idempotencyKey\":\"k1\",\"accountId\":1,"
                                                + "\"amount\":10,\"currency\":\"ZAR\","
                                                + "\"reference\":\"r\"}"))
                .andExpect(status().isOk());
    }

    private String as(String userId, String role) {
        return tokens.mint(userId, List.of(role), null);
    }

    private AccountResponse account(Long customerId) {
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
