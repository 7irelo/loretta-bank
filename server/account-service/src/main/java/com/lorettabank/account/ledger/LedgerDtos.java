package com.lorettabank.account.ledger;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class LedgerDtos {

    private LedgerDtos() {}

    public record CashRequest(
            @NotBlank @Size(max = 255) String idempotencyKey,
            @NotNull Long accountId,
            @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotBlank @Size(max = 64) String reference,
            @Size(max = 255) String description) {}

    public record TransferRequest(
            @NotBlank @Size(max = 255) String idempotencyKey,
            @NotNull Long sourceAccountId,
            @NotNull Long targetAccountId,
            @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotBlank @Size(max = 64) String reference,
            @Size(max = 255) String description) {}

    public record Entry(
            Long accountId, String systemAccount, BigDecimal amount, BigDecimal balanceAfter) {}

    public record JournalResponse(
            UUID journalId,
            String journalType,
            String reference,
            String currency,
            Instant createdAt,
            boolean replayed,
            List<Entry> entries) {}

    public record BalanceAt(Long accountId, Instant at, BigDecimal balance, String currency) {}

    public record ReconciliationReport(
            Instant checkedAt,
            BigDecimal ledgerTotal,
            List<Long> accountsWithDrift,
            boolean healthy) {}
}
