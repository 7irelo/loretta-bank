package com.lorettabank.account.controller;

import com.lorettabank.account.dto.AccountResponse;
import com.lorettabank.account.ledger.LedgerDtos.CashRequest;
import com.lorettabank.account.ledger.LedgerDtos.JournalResponse;
import com.lorettabank.account.ledger.LedgerDtos.TransferRequest;
import com.lorettabank.account.ledger.LedgerService;
import com.lorettabank.account.service.AccountService;
import jakarta.validation.Valid;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service ledger API. The {@code /internal/**} prefix is not routed by the gateway and
 * requires the SERVICE role (see ServiceSecurityConfig).
 */
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalLedgerController {

    private final LedgerService ledgerService;
    private final AccountService accountService;

    @PostMapping("/ledger/cash-deposits")
    public JournalResponse cashDeposit(@Valid @RequestBody CashRequest request) {
        return idempotent(
                request.idempotencyKey(), () -> ledgerService.cashDeposit(request, actor()));
    }

    @PostMapping("/ledger/cash-withdrawals")
    public JournalResponse cashWithdrawal(@Valid @RequestBody CashRequest request) {
        return idempotent(
                request.idempotencyKey(), () -> ledgerService.cashWithdrawal(request, actor()));
    }

    @PostMapping("/ledger/transfers")
    public JournalResponse transfer(@Valid @RequestBody TransferRequest request) {
        return idempotent(request.idempotencyKey(), () -> ledgerService.transfer(request, actor()));
    }

    @GetMapping("/accounts/{id}")
    public AccountResponse account(@PathVariable Long id) {
        return accountService.getAccount(id);
    }

    @GetMapping("/accounts/by-number/{accountNumber}")
    public AccountResponse accountByNumber(@PathVariable String accountNumber) {
        return accountService.getAccountByNumber(accountNumber);
    }

    /** Two concurrent requests with one key: the loser hits the unique index and replays. */
    private JournalResponse idempotent(String key, Supplier<JournalResponse> action) {
        try {
            return action.get();
        } catch (DataIntegrityViolationException e) {
            return ledgerService.findByIdempotencyKey(key).orElseThrow(() -> e);
        }
    }

    private String actor() {
        return String.valueOf(
                SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
}
