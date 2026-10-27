package com.lorettabank.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorettabank.shared.dto.PagedResponse;
import com.lorettabank.shared.event.TransferCompletedEvent;
import com.lorettabank.shared.event.TransferFailedEvent;
import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.exception.DuplicateResourceException;
import com.lorettabank.shared.exception.ResourceNotFoundException;
import com.lorettabank.transaction.client.AccountClient;
import com.lorettabank.transaction.client.AccountClient.JournalResponse;
import com.lorettabank.transaction.client.LedgerUnavailableException;
import com.lorettabank.transaction.dto.LedgerEntryResponse;
import com.lorettabank.transaction.dto.TransactionResponse;
import com.lorettabank.transaction.entity.EntryType;
import com.lorettabank.transaction.entity.LedgerEntry;
import com.lorettabank.transaction.entity.OutboxEvent;
import com.lorettabank.transaction.entity.Transaction;
import com.lorettabank.transaction.entity.TransactionStatus;
import com.lorettabank.transaction.entity.TransactionType;
import com.lorettabank.transaction.mapper.TransactionMapper;
import com.lorettabank.transaction.repository.LedgerEntryRepository;
import com.lorettabank.transaction.repository.OutboxEventRepository;
import com.lorettabank.transaction.repository.TransactionRepository;
import com.lorettabank.transaction.repository.TransactionSpecifications;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Owns the lifecycle of money instructions. Each instruction is committed as PENDING before the
 * ledger is called, so a crash at any point leaves a record that {@link PendingTransactionRecovery}
 * finishes by replaying the same idempotency key against the ledger.
 */
@Service
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountClient accountClient;
    private final TransactionMapper transactionMapper;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;

    public TransactionService(
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            OutboxEventRepository outboxEventRepository,
            AccountClient accountClient,
            TransactionMapper transactionMapper,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.accountClient = accountClient;
        this.transactionMapper = transactionMapper;
        this.objectMapper = objectMapper;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /** A validated instruction, ready to be recorded and executed. */
    public record Instruction(
            TransactionType type,
            String idempotencyKey,
            Long sourceAccountId,
            Long targetAccountId,
            Long sourceCustomerId,
            Long targetCustomerId,
            BigDecimal amount,
            String currency,
            String description,
            String initiatedBy) {}

    public Optional<Transaction> findByIdempotencyKey(String key) {
        return transactionRepository.findByIdempotencyKey(key);
    }

    /**
     * Records the instruction as PENDING and executes it. Business rejections end as FAILED and are
     * rethrown; an unreachable ledger leaves it PENDING for the recovery job.
     */
    public TransactionResponse execute(Instruction instruction) {
        Optional<Transaction> existing =
                transactionRepository.findByIdempotencyKey(instruction.idempotencyKey());
        if (existing.isPresent()) {
            Transaction found = existing.get();
            if (!sameInstruction(found, instruction)) {
                throw new DuplicateResourceException(
                        "Idempotency key was already used for a different request");
            }
            return toResponse(found);
        }

        Transaction pending =
                tx.execute(
                        status ->
                                transactionRepository.save(
                                        Transaction.builder()
                                                .id(UUID.randomUUID().toString())
                                                .type(instruction.type())
                                                .status(TransactionStatus.PENDING)
                                                .sourceAccountId(instruction.sourceAccountId())
                                                .targetAccountId(instruction.targetAccountId())
                                                .sourceCustomerId(instruction.sourceCustomerId())
                                                .targetCustomerId(instruction.targetCustomerId())
                                                .amount(instruction.amount())
                                                .currency(instruction.currency())
                                                .description(instruction.description())
                                                .reference(referenceFor(instruction.type()))
                                                .idempotencyKey(instruction.idempotencyKey())
                                                .initiatedBy(instruction.initiatedBy())
                                                .build()));

        return toResponse(advance(Objects.requireNonNull(pending).getId(), true));
    }

    /**
     * Calls the ledger for a PENDING transaction and records the outcome. Safe to call any number
     * of times: the ledger deduplicates on the idempotency key.
     */
    public Transaction advance(String transactionId, boolean rethrowBusinessFailure) {
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            return transaction;
        }
        try {
            JournalResponse journal = postToLedger(transaction);
            return tx.execute(status -> complete(transactionId, journal));
        } catch (BusinessException | ResourceNotFoundException e) {
            Transaction failed = tx.execute(status -> fail(transactionId, e.getMessage()));
            if (rethrowBusinessFailure) {
                throw e;
            }
            return failed;
        } catch (LedgerUnavailableException e) {
            log.warn("Ledger unavailable for transaction {}; will retry", transactionId);
            tx.executeWithoutResult(
                    status ->
                            transactionRepository
                                    .findById(transactionId)
                                    .ifPresent(t -> t.setAttempts(t.getAttempts() + 1)));
            return transactionRepository.findById(transactionId).orElseThrow();
        }
    }

    private JournalResponse postToLedger(Transaction t) {
        String key = "txn:" + t.getIdempotencyKey();
        return switch (t.getType()) {
            case DEPOSIT ->
                    accountClient.cashDeposit(
                            key,
                            t.getTargetAccountId(),
                            t.getAmount(),
                            t.getCurrency(),
                            t.getReference(),
                            t.getDescription());
            case WITHDRAWAL ->
                    accountClient.cashWithdrawal(
                            key,
                            t.getSourceAccountId(),
                            t.getAmount(),
                            t.getCurrency(),
                            t.getReference(),
                            t.getDescription());
            case TRANSFER ->
                    accountClient.transfer(
                            key,
                            t.getSourceAccountId(),
                            t.getTargetAccountId(),
                            t.getAmount(),
                            t.getCurrency(),
                            t.getReference(),
                            t.getDescription());
        };
    }

    private Transaction complete(String transactionId, JournalResponse journal) {
        Transaction t = transactionRepository.findById(transactionId).orElseThrow();
        if (t.getStatus() != TransactionStatus.PENDING) {
            return t;
        }
        t.setStatus(TransactionStatus.COMPLETED);
        t.setJournalId(journal.journalId());
        transactionRepository.save(t);

        for (AccountClient.LedgerEntry entry : journal.entries()) {
            if (entry.accountId() == null) {
                continue;
            }
            ledgerEntryRepository.save(
                    LedgerEntry.builder()
                            .transactionId(t.getId())
                            .accountId(entry.accountId())
                            .entryType(
                                    entry.amount().signum() < 0
                                            ? EntryType.DEBIT
                                            : EntryType.CREDIT)
                            .amount(entry.amount().abs())
                            .currency(t.getCurrency())
                            .balanceAfter(entry.balanceAfter())
                            .description(t.getDescription())
                            .build());
        }

        if (t.getType() == TransactionType.TRANSFER) {
            TransferCompletedEvent event =
                    TransferCompletedEvent.builder()
                            .eventType("TRANSFER_COMPLETED")
                            .aggregateId(t.getId())
                            .transferId(t.getId())
                            .sourceAccountId(t.getSourceAccountId())
                            .targetAccountId(t.getTargetAccountId())
                            .sourceCustomerId(t.getSourceCustomerId())
                            .targetCustomerId(t.getTargetCustomerId())
                            .amount(t.getAmount())
                            .currency(t.getCurrency())
                            .build();
            event.initDefaults();
            saveOutboxEvent(t.getId(), "TRANSFER_COMPLETED", event);
        }
        log.info(
                "Transaction {} ({}) completed, journal {}",
                t.getId(),
                t.getType(),
                journal.journalId());
        return t;
    }

    private Transaction fail(String transactionId, String reason) {
        Transaction t = transactionRepository.findById(transactionId).orElseThrow();
        if (t.getStatus() != TransactionStatus.PENDING) {
            return t;
        }
        t.setStatus(TransactionStatus.FAILED);
        t.setFailureReason(reason);
        transactionRepository.save(t);

        if (t.getType() == TransactionType.TRANSFER) {
            TransferFailedEvent event =
                    TransferFailedEvent.builder()
                            .eventType("TRANSFER_FAILED")
                            .aggregateId(t.getId())
                            .transferId(t.getId())
                            .sourceAccountId(t.getSourceAccountId())
                            .targetAccountId(t.getTargetAccountId())
                            .sourceCustomerId(t.getSourceCustomerId())
                            .targetCustomerId(t.getTargetCustomerId())
                            .amount(t.getAmount())
                            .currency(t.getCurrency())
                            .reason(reason)
                            .build();
            event.initDefaults();
            saveOutboxEvent(t.getId(), "TRANSFER_FAILED", event);
        }
        log.info("Transaction {} failed: {}", t.getId(), reason);
        return t;
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String transactionId) {
        return toResponse(
                transactionRepository
                        .findById(transactionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Transaction not found")));
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> search(
            Collection<Long> accountIds,
            TransactionType type,
            Instant from,
            Instant to,
            String text,
            int page,
            int size) {
        Page<Transaction> result =
                transactionRepository.findAll(
                        TransactionSpecifications.matching(accountIds, type, from, to, text),
                        PageRequest.of(
                                Math.max(page, 0),
                                Math.min(Math.max(size, 1), 200),
                                Sort.by(Sort.Direction.DESC, "createdAt")));
        return PagedResponse.<TransactionResponse>builder()
                .content(
                        result.getContent().stream()
                                .map(transactionMapper::toTransactionResponse)
                                .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<LedgerEntryResponse> getLedgerEntriesForAccount(
            Long accountId, int page, int size) {
        var entryPage =
                ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(
                        accountId,
                        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200)));
        return PagedResponse.<LedgerEntryResponse>builder()
                .content(
                        entryPage.getContent().stream()
                                .map(transactionMapper::toLedgerEntryResponse)
                                .toList())
                .page(entryPage.getNumber())
                .size(entryPage.getSize())
                .totalElements(entryPage.getTotalElements())
                .totalPages(entryPage.getTotalPages())
                .last(entryPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public BigDecimal outgoingSince(Long customerId, Instant since) {
        return transactionRepository.sumOutgoingSince(customerId, since);
    }

    private TransactionResponse toResponse(Transaction transaction) {
        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(transaction.getId());
        return transactionMapper.toTransactionResponse(transaction, entries);
    }

    private static boolean sameInstruction(Transaction t, Instruction i) {
        return t.getType() == i.type()
                && Objects.equals(t.getSourceAccountId(), i.sourceAccountId())
                && Objects.equals(t.getTargetAccountId(), i.targetAccountId())
                && t.getAmount().compareTo(i.amount()) == 0
                && Objects.equals(t.getInitiatedBy(), i.initiatedBy());
    }

    private static String referenceFor(TransactionType type) {
        String prefix =
                switch (type) {
                    case DEPOSIT -> "DEP-";
                    case WITHDRAWAL -> "WDR-";
                    case TRANSFER -> "TRF-";
                };
        return prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void saveOutboxEvent(String aggregateId, String eventType, Object event) {
        try {
            outboxEventRepository.save(
                    OutboxEvent.builder()
                            .aggregateType("Transfer")
                            .aggregateId(aggregateId)
                            .eventType(eventType)
                            .payload(objectMapper.writeValueAsString(event))
                            .build());
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to serialize event: " + e.getMessage());
        }
    }
}
