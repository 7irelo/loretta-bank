package com.lorettabank.transaction.service;

import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.exception.ForbiddenException;
import com.lorettabank.transaction.client.CustomerClient.CustomerSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Customer-facing payment rules: verified identity, per-payment and daily outgoing limits. */
@Component
public class PaymentPolicy {

    private static final ZoneId BANK_ZONE = ZoneId.of("Africa/Johannesburg");

    private final BigDecimal perTransactionLimit;
    private final BigDecimal dailyLimit;
    private final TransactionService transactionService;

    public PaymentPolicy(
            @Value("${payments.limits.per-transaction:50000}") BigDecimal perTransactionLimit,
            @Value("${payments.limits.daily:100000}") BigDecimal dailyLimit,
            TransactionService transactionService) {
        this.perTransactionLimit = perTransactionLimit;
        this.dailyLimit = dailyLimit;
        this.transactionService = transactionService;
    }

    public void assertCustomerMayPay(CustomerSummary customer, BigDecimal amount) {
        if (!customer.verified()) {
            throw new ForbiddenException(
                    "Your identity must be verified before you can move money");
        }
        if (amount.compareTo(perTransactionLimit) > 0) {
            throw new BusinessException(
                    "Amount exceeds the per-payment limit of R"
                            + perTransactionLimit.toPlainString());
        }
        Instant startOfDay = LocalDate.now(BANK_ZONE).atStartOfDay(BANK_ZONE).toInstant();
        BigDecimal usedToday = transactionService.outgoingSince(customer.id(), startOfDay);
        if (usedToday.add(amount).compareTo(dailyLimit) > 0) {
            throw new BusinessException(
                    "Amount exceeds your remaining daily limit of R"
                            + dailyLimit.subtract(usedToday).max(BigDecimal.ZERO).toPlainString());
        }
    }
}
