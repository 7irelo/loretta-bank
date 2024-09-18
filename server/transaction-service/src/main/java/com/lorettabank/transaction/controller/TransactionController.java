package com.lorettabank.transaction.controller;

import com.lorettabank.shared.dto.PagedResponse;
import com.lorettabank.shared.exception.ForbiddenException;
import com.lorettabank.shared.security.JwtConstants;
import com.lorettabank.transaction.client.AccountClient;
import com.lorettabank.transaction.client.CustomerClient;
import com.lorettabank.transaction.dto.AccountBalanceResponse;
import com.lorettabank.transaction.dto.DepositRequest;
import com.lorettabank.transaction.dto.LedgerEntryResponse;
import com.lorettabank.transaction.dto.TransactionResponse;
import com.lorettabank.transaction.dto.WithdrawRequest;
import com.lorettabank.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Deposit, withdrawal, and transaction query operations")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountClient accountClient;
    private final CustomerClient customerClient;

    public TransactionController(
            TransactionService transactionService,
            AccountClient accountClient,
            CustomerClient customerClient) {
        this.transactionService = transactionService;
        this.accountClient = accountClient;
        this.customerClient = customerClient;
    }

    @PostMapping("/deposit")
    @Operation(summary = "Record a deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        assertWriteAllowed();
        assertCustomerOwnsAccount(request.getAccountId());
        TransactionResponse response = transactionService.recordDeposit(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Record a withdrawal")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        assertWriteAllowed();
        assertCustomerOwnsAccount(request.getAccountId());
        TransactionResponse response = transactionService.recordWithdrawal(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String id) {
        TransactionResponse response = transactionService.getTransaction(id);
        if (hasRole(JwtConstants.ROLE_CUSTOMER)) {
            if (response.getSourceAccountId() != null) {
                assertCustomerOwnsAccount(response.getSourceAccountId());
            }
            if (response.getTargetAccountId() != null) {
                assertCustomerOwnsAccount(response.getTargetAccountId());
            }
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get transactions for an account")
    public ResponseEntity<PagedResponse<TransactionResponse>> getTransactionsForAccount(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        assertCustomerOwnsAccount(accountId);
        return ResponseEntity.ok(
                transactionService.getTransactionsForAccount(accountId, page, size));
    }

    @GetMapping("/account/{accountId}/ledger")
    @Operation(summary = "Get ledger entries for an account")
    public ResponseEntity<PagedResponse<LedgerEntryResponse>> getLedgerEntries(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        assertCustomerOwnsAccount(accountId);
        return ResponseEntity.ok(
                transactionService.getLedgerEntriesForAccount(accountId, page, size));
    }

    private void assertCustomerOwnsAccount(Long accountId) {
        if (!hasRole(JwtConstants.ROLE_CUSTOMER)) {
            return;
        }

        AccountBalanceResponse account = accountClient.getAccount(accountId);
        Long authenticatedCustomerId = getAuthenticatedCustomerId();
        if (account.getCustomerId() == null
                || !account.getCustomerId().equals(authenticatedCustomerId)) {
            throw new ForbiddenException("You can only access your own account transactions");
        }
    }

    private void assertWriteAllowed() {
        if (!hasRole(JwtConstants.ROLE_ADMIN) && !hasRole(JwtConstants.ROLE_CUSTOMER)) {
            throw new ForbiddenException("Only ADMIN or CUSTOMER can create transactions");
        }
    }

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }

    private Long getAuthenticatedCustomerId() {
        return customerClient.resolveCurrentCustomerId(getAuthenticatedUserId(), getRolesHeader());
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
