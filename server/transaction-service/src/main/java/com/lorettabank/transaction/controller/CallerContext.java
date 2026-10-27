package com.lorettabank.transaction.controller;

import com.lorettabank.shared.exception.ForbiddenException;
import com.lorettabank.shared.security.JwtConstants;
import com.lorettabank.transaction.client.AccountClient;
import com.lorettabank.transaction.client.CustomerClient;
import com.lorettabank.transaction.client.CustomerClient.CustomerSummary;
import com.lorettabank.transaction.dto.AccountBalanceResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Who is calling, and which accounts they may touch. */
@Component
@RequiredArgsConstructor
public class CallerContext {

    private final AccountClient accountClient;
    private final CustomerClient customerClient;

    public String userId() {
        return String.valueOf(auth().getPrincipal());
    }

    public boolean hasRole(String role) {
        return auth().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    public boolean isStaff() {
        return hasRole(JwtConstants.ROLE_ADMIN) || hasRole(JwtConstants.ROLE_SUPPORT);
    }

    public CustomerSummary customer() {
        if (!hasRole(JwtConstants.ROLE_CUSTOMER)) {
            throw new ForbiddenException("Only customers can perform this action");
        }
        return customerClient.currentCustomer(Long.valueOf(userId()), rolesHeader());
    }

    public List<Long> customerAccountIds() {
        return accountClient.getAccountsForCustomer(customer().id()).stream()
                .map(AccountBalanceResponse::getId)
                .toList();
    }

    /** Staff may read any account; customers only their own. Returns the account. */
    public AccountBalanceResponse assertCanRead(Long accountId) {
        AccountBalanceResponse account = accountClient.getAccount(accountId);
        if (isStaff()) {
            return account;
        }
        assertOwns(account);
        return account;
    }

    public void assertOwns(AccountBalanceResponse account) {
        if (account.getCustomerId() == null || !account.getCustomerId().equals(customer().id())) {
            throw new ForbiddenException("You can only access your own accounts");
        }
    }

    private String rolesHeader() {
        return auth().getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private static Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
