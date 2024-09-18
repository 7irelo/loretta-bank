package com.lorettabank.account.controller;

import com.lorettabank.account.client.CustomerIdentityClient;
import com.lorettabank.account.dto.AccountResponse;
import com.lorettabank.account.dto.CreateAccountRequest;
import com.lorettabank.account.dto.DepositRequest;
import com.lorettabank.account.dto.WithdrawRequest;
import com.lorettabank.account.service.AccountService;
import com.lorettabank.shared.exception.ForbiddenException;
import com.lorettabank.shared.security.JwtConstants;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final CustomerIdentityClient customerIdentityClient;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        if (hasRole(JwtConstants.ROLE_CUSTOMER)
                && !request.getCustomerId().equals(getAuthenticatedCustomerId())) {
            throw new ForbiddenException("Customers can only open accounts for themselves");
        }
        if (!hasRole(JwtConstants.ROLE_ADMIN) && !hasRole(JwtConstants.ROLE_CUSTOMER)) {
            throw new ForbiddenException("Only ADMIN or CUSTOMER can open accounts");
        }

        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
        AccountResponse response = accountService.getAccount(id);

        if (hasRole(JwtConstants.ROLE_ADMIN) || hasRole(JwtConstants.ROLE_SUPPORT)) {
            return ResponseEntity.ok(response);
        }

        if (hasRole(JwtConstants.ROLE_CUSTOMER)
                && response.getCustomerId().equals(getAuthenticatedCustomerId())) {
            return ResponseEntity.ok(response);
        }

        throw new ForbiddenException("You do not have permission to view this account");
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomer(
            @PathVariable Long customerId) {
        if (hasRole(JwtConstants.ROLE_CUSTOMER)
                && !customerId.equals(getAuthenticatedCustomerId())) {
            throw new ForbiddenException("Customers can only view their own accounts");
        }
        if (!hasRole(JwtConstants.ROLE_ADMIN)
                && !hasRole(JwtConstants.ROLE_SUPPORT)
                && !hasRole(JwtConstants.ROLE_CUSTOMER)) {
            throw new ForbiddenException("Only ADMIN, SUPPORT, or CUSTOMER can view accounts");
        }

        List<AccountResponse> responses = accountService.getAccountsByCustomer(customerId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable Long id, @Valid @RequestBody DepositRequest request) {
        AccountResponse account = accountService.getAccount(id);
        if (hasRole(JwtConstants.ROLE_CUSTOMER)
                && !account.getCustomerId().equals(getAuthenticatedCustomerId())) {
            throw new ForbiddenException("Customers can only deposit to their own accounts");
        }
        if (!hasRole(JwtConstants.ROLE_ADMIN) && !hasRole(JwtConstants.ROLE_CUSTOMER)) {
            throw new ForbiddenException("Only ADMIN or CUSTOMER can perform deposits");
        }

        AccountResponse response = accountService.deposit(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(
            @PathVariable Long id, @Valid @RequestBody WithdrawRequest request) {
        AccountResponse account = accountService.getAccount(id);
        if (hasRole(JwtConstants.ROLE_CUSTOMER)
                && !account.getCustomerId().equals(getAuthenticatedCustomerId())) {
            throw new ForbiddenException("Customers can only withdraw from their own accounts");
        }
        if (!hasRole(JwtConstants.ROLE_ADMIN) && !hasRole(JwtConstants.ROLE_CUSTOMER)) {
            throw new ForbiddenException("Only ADMIN or CUSTOMER can perform withdrawals");
        }

        AccountResponse response = accountService.withdraw(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/freeze")
    public ResponseEntity<AccountResponse> freezeAccount(@PathVariable Long id) {
        if (!hasRole(JwtConstants.ROLE_ADMIN)) {
            throw new ForbiddenException("Only admins can freeze accounts");
        }

        AccountResponse response = accountService.freezeAccount(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<AccountResponse> closeAccount(@PathVariable Long id) {
        if (!hasRole(JwtConstants.ROLE_ADMIN)) {
            throw new ForbiddenException("Only admins can close accounts");
        }

        AccountResponse response = accountService.closeAccount(id);
        return ResponseEntity.ok(response);
    }

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }

    private Long getAuthenticatedCustomerId() {
        if (!hasRole(JwtConstants.ROLE_CUSTOMER)) {
            return null;
        }

        return customerIdentityClient.resolveCurrentCustomerId(
                getAuthenticatedUserId(), getRolesHeader());
    }

    private String getRolesHeader() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
