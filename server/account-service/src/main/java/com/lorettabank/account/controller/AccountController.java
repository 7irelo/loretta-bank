package com.lorettabank.account.controller;

import com.lorettabank.account.client.CustomerIdentityClient;
import com.lorettabank.account.client.CustomerIdentityClient.CustomerSummary;
import com.lorettabank.account.dto.AccountResponse;
import com.lorettabank.account.dto.CreateAccountRequest;
import com.lorettabank.account.ledger.LedgerDtos.BalanceAt;
import com.lorettabank.account.ledger.LedgerDtos.ReconciliationReport;
import com.lorettabank.account.ledger.LedgerService;
import com.lorettabank.account.service.AccountService;
import com.lorettabank.shared.dto.PagedResponse;
import com.lorettabank.shared.exception.ForbiddenException;
import com.lorettabank.shared.security.JwtConstants;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public account API. Money never moves through this controller; deposits, withdrawals and
 * transfers go through transaction-service, which posts journals to the ledger.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final LedgerService ledgerService;
    private final CustomerIdentityClient customerIdentityClient;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        if (hasRole(JwtConstants.ROLE_CUSTOMER) && !hasRole(JwtConstants.ROLE_ADMIN)) {
            CustomerSummary customer = currentCustomer();
            if (!request.getCustomerId().equals(customer.id())) {
                throw new ForbiddenException("Customers can only open accounts for themselves");
            }
            if (!customer.verified()) {
                throw new ForbiddenException(
                        "Your identity must be verified before you can open an account");
            }
        } else if (!hasRole(JwtConstants.ROLE_ADMIN)) {
            throw new ForbiddenException("Only ADMIN or CUSTOMER can open accounts");
        }

        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<AccountResponse>> listAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        assertStaff();
        return ResponseEntity.ok(accountService.listAccounts(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
        AccountResponse response = accountService.getAccount(id);
        assertCanRead(response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceAt> getBalanceAt(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at) {
        assertCanRead(accountService.getAccount(id));
        return ResponseEntity.ok(ledgerService.balanceAt(id, at));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomer(
            @PathVariable Long customerId) {
        if (!isStaffOrService()) {
            if (!hasRole(JwtConstants.ROLE_CUSTOMER)
                    || !customerId.equals(currentCustomer().id())) {
                throw new ForbiddenException("Customers can only view their own accounts");
            }
        }
        return ResponseEntity.ok(accountService.getAccountsByCustomer(customerId));
    }

    @PatchMapping("/{id}/freeze")
    public ResponseEntity<AccountResponse> freezeAccount(@PathVariable Long id) {
        assertAdmin();
        return ResponseEntity.ok(accountService.freezeAccount(id));
    }

    @PatchMapping("/{id}/unfreeze")
    public ResponseEntity<AccountResponse> unfreezeAccount(@PathVariable Long id) {
        assertAdmin();
        return ResponseEntity.ok(accountService.unfreezeAccount(id));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<AccountResponse> closeAccount(@PathVariable Long id) {
        assertAdmin();
        return ResponseEntity.ok(accountService.closeAccount(id));
    }

    @GetMapping("/admin/reconciliation")
    public ResponseEntity<ReconciliationReport> reconcile() {
        assertAdmin();
        return ResponseEntity.ok(ledgerService.reconcile());
    }

    private void assertCanRead(AccountResponse account) {
        if (isStaffOrService()) {
            return;
        }
        if (hasRole(JwtConstants.ROLE_CUSTOMER)
                && account.getCustomerId().equals(currentCustomer().id())) {
            return;
        }
        throw new ForbiddenException("You do not have permission to view this account");
    }

    private void assertAdmin() {
        if (!hasRole(JwtConstants.ROLE_ADMIN)) {
            throw new ForbiddenException("Only admins can perform this action");
        }
    }

    private void assertStaff() {
        if (!hasRole(JwtConstants.ROLE_ADMIN) && !hasRole(JwtConstants.ROLE_SUPPORT)) {
            throw new ForbiddenException("Only ADMIN or SUPPORT can list accounts");
        }
    }

    private boolean isStaffOrService() {
        return hasRole(JwtConstants.ROLE_ADMIN)
                || hasRole(JwtConstants.ROLE_SUPPORT)
                || hasRole(JwtConstants.ROLE_SERVICE);
    }

    private CustomerSummary currentCustomer() {
        return customerIdentityClient.currentCustomer(getAuthenticatedUserId(), getRolesHeader());
    }

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
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
