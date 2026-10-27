package com.lorettabank.transaction.service;

import com.lorettabank.transaction.entity.Transaction;
import com.lorettabank.transaction.entity.TransactionStatus;
import com.lorettabank.transaction.repository.TransactionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Finishes instructions left PENDING by a crash or an unreachable ledger. Replays are safe because
 * the ledger deduplicates journals on the idempotency key.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PendingTransactionRecovery {

    private static final int ALERT_AFTER_ATTEMPTS = 20;

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final PlatformTransactionManager transactionManager;

    @Scheduled(fixedDelay = 15_000, initialDelay = 15_000)
    public void recover() {
        List<String> ids =
                new TransactionTemplate(transactionManager)
                        .execute(status -> transactionRepository.claimStalePending(25));
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            Transaction result = transactionService.advance(id, false);
            if (result.getStatus() == TransactionStatus.PENDING
                    && result.getAttempts() >= ALERT_AFTER_ATTEMPTS) {
                log.error(
                        "Transaction {} still PENDING after {} attempts; needs attention",
                        id,
                        result.getAttempts());
            }
        }
    }
}
