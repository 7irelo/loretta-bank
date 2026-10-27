package com.lorettabank.transaction.controller;

import com.lorettabank.shared.dto.PagedResponse;
import com.lorettabank.shared.exception.ForbiddenException;
import com.lorettabank.shared.security.JwtConstants;
import com.lorettabank.transaction.client.AccountClient;
import com.lorettabank.transaction.client.CustomerClient.CustomerSummary;
import com.lorettabank.transaction.dto.AccountBalanceResponse;
import com.lorettabank.transaction.dto.DepositRequest;
import com.lorettabank.transaction.dto.LedgerEntryResponse;
import com.lorettabank.transaction.dto.TransactionResponse;
import com.lorettabank.transaction.dto.WithdrawRequest;
import com.lorettabank.transaction.entity.TransactionType;
import com.lorettabank.transaction.service.PaymentPolicy;
import com.lorettabank.transaction.service.TransactionService;
import com.lorettabank.transaction.service.TransactionService.Instruction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
@Tag(name = "Transactions", description = "Deposits, withdrawals and transaction history")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountClient accountClient;
    private final CallerContext caller;
    private final PaymentPolicy paymentPolicy;

    /** Cash deposits are recorded by staff at a branch; customers cannot create money. */
    @PostMapping("/deposit")
    @Operation(summary = "Record a cash deposit (ADMIN)")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            @RequestHeader("Idempotency-Key") @Size(min = 8, max = 128) String idempotencyKey) {
        if (!caller.hasRole(JwtConstants.ROLE_ADMIN)) {
            throw new ForbiddenException("Only staff can record cash deposits");
        }
        AccountBalanceResponse account = accountClient.getAccount(request.getAccountId());
        TransactionResponse response =
                transactionService.execute(
                        new Instruction(
                                TransactionType.DEPOSIT,
                                scoped(idempotencyKey),
                                null,
                                account.getId(),
                                null,
                                account.getCustomerId(),
                                request.getAmount(),
                                request.getCurrency(),
                                request.getDescription(),
                                caller.userId()));
        return ResponseEntity.status(statusFor(response)).body(response);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw cash from your own account")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            @RequestHeader("Idempotency-Key") @Size(min = 8, max = 128) String idempotencyKey) {
        AccountBalanceResponse account = accountClient.getAccount(request.getAccountId());
        Long customerId = account.getCustomerId();
        if (!caller.hasRole(JwtConstants.ROLE_ADMIN)) {
            caller.assertOwns(account);
            CustomerSummary customer = caller.customer();
            paymentPolicy.assertCustomerMayPay(customer, request.getAmount());
        }
        TransactionResponse response =
                transactionService.execute(
                        new Instruction(
                                TransactionType.WITHDRAWAL,
                                scoped(idempotencyKey),
                                account.getId(),
                                null,
                                customerId,
                                null,
                                request.getAmount(),
                                request.getCurrency(),
                                request.getDescription(),
                                caller.userId()));
        return ResponseEntity.status(statusFor(response)).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String id) {
        TransactionResponse response = transactionService.getTransaction(id);
        if (!caller.isStaff()) {
            List<Long> mine = caller.customerAccountIds();
            boolean involved =
                    mine.contains(response.getSourceAccountId())
                            || mine.contains(response.getTargetAccountId());
            if (!involved) {
                throw new ForbiddenException("You can only view your own transactions");
            }
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mine")
    @Operation(summary = "Search the caller's transactions across all their accounts")
    public ResponseEntity<PagedResponse<TransactionResponse>> mine(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Long> accountIds = caller.customerAccountIds();
        if (accountIds.isEmpty()) {
            return ResponseEntity.ok(
                    PagedResponse.<TransactionResponse>builder()
                            .content(List.of())
                            .page(0)
                            .size(size)
                            .totalElements(0)
                            .totalPages(0)
                            .last(true)
                            .build());
        }
        return ResponseEntity.ok(
                transactionService.search(accountIds, type, from, to, q, page, size));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Search transactions for one account")
    public ResponseEntity<PagedResponse<TransactionResponse>> getTransactionsForAccount(
            @PathVariable Long accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        caller.assertCanRead(accountId);
        return ResponseEntity.ok(
                transactionService.search(List.of(accountId), type, from, to, q, page, size));
    }

    @GetMapping("/account/{accountId}/ledger")
    @Operation(summary = "Get ledger entries for an account")
    public ResponseEntity<PagedResponse<LedgerEntryResponse>> getLedgerEntries(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        caller.assertCanRead(accountId);
        return ResponseEntity.ok(
                transactionService.getLedgerEntriesForAccount(accountId, page, size));
    }

    /** Keys are namespaced per caller so one user cannot replay or probe another's key. */
    private String scoped(String idempotencyKey) {
        return caller.userId() + ":" + idempotencyKey;
    }

    static HttpStatus statusFor(TransactionResponse response) {
        return "PENDING".equals(response.getStatus()) ? HttpStatus.ACCEPTED : HttpStatus.CREATED;
    }
}
