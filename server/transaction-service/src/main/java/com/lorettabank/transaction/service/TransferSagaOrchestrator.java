package com.lorettabank.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorettabank.shared.event.TransferCompletedEvent;
import com.lorettabank.shared.event.TransferFailedEvent;
import com.lorettabank.shared.event.TransferInitiatedEvent;
import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.transaction.client.AccountClient;
import com.lorettabank.transaction.dto.AccountBalanceResponse;
import com.lorettabank.transaction.entity.EntryType;
import com.lorettabank.transaction.entity.LedgerEntry;
import com.lorettabank.transaction.entity.OutboxEvent;
import com.lorettabank.transaction.entity.SagaStatus;
import com.lorettabank.transaction.entity.Transaction;
import com.lorettabank.transaction.entity.TransactionStatus;
import com.lorettabank.transaction.entity.TransactionType;
import com.lorettabank.transaction.entity.TransferSaga;
import com.lorettabank.transaction.repository.LedgerEntryRepository;
import com.lorettabank.transaction.repository.OutboxEventRepository;
import com.lorettabank.transaction.repository.TransactionRepository;
import com.lorettabank.transaction.repository.TransferSagaRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TransferSagaOrchestrator.class);

    private final TransferSagaRepository transferSagaRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountClient accountClient;
    private final ObjectMapper objectMapper;

    public TransferSagaOrchestrator(
            TransferSagaRepository transferSagaRepository,
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            OutboxEventRepository outboxEventRepository,
            AccountClient accountClient,
            ObjectMapper objectMapper) {
        this.transferSagaRepository = transferSagaRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.accountClient = accountClient;
        this.objectMapper = objectMapper;
    }

    public void execute(TransferSaga saga) {
        log.info(
                "Starting transfer saga: id={}, source={}, target={}, amount={}",
                saga.getId(),
                saga.getSourceAccountId(),
                saga.getTargetAccountId(),
                saga.getAmount());

        saveInitiatedEvent(saga);

        String reference = "TRF-" + saga.getId().substring(0, 8).toUpperCase();

        try {
            stepDebitSource(saga, reference);
        } catch (Exception e) {
            log.error(
                    "Transfer saga failed at debit step: sagaId={}, reason={}",
                    saga.getId(),
                    e.getMessage());
            failSaga(saga, "Debit failed: " + e.getMessage());
            return;
        }

        try {
            stepCreditTarget(saga, reference);
        } catch (Exception e) {
            log.error(
                    "Transfer saga failed at credit step, compensating: sagaId={}, reason={}",
                    saga.getId(),
                    e.getMessage());
            compensate(saga, reference, "Credit failed: " + e.getMessage());
            return;
        }

        completeSaga(saga, reference);
    }

    private void stepDebitSource(TransferSaga saga, String reference) {
        log.debug("Saga step: debit source accountId={}", saga.getSourceAccountId());
        accountClient.debit(saga.getSourceAccountId(), saga.getAmount(), reference);
        saga.setStatus(SagaStatus.DEBITED);
        transferSagaRepository.save(saga);
        log.debug("Saga step completed: source debited, sagaId={}", saga.getId());
    }

    private void stepCreditTarget(TransferSaga saga, String reference) {
        log.debug("Saga step: credit target accountId={}", saga.getTargetAccountId());
        accountClient.credit(saga.getTargetAccountId(), saga.getAmount(), reference);
        log.debug("Saga step completed: target credited, sagaId={}", saga.getId());
    }

    @Transactional
    private void compensate(TransferSaga saga, String reference, String reason) {
        log.warn(
                "Compensating transfer saga: sagaId={}, reason={}",
                saga.getId(),
                reason);

        saga.setStatus(SagaStatus.COMPENSATING);
        transferSagaRepository.save(saga);

        try {
            String compensationRef = "COMP-" + reference;
            accountClient.credit(saga.getSourceAccountId(), saga.getAmount(), compensationRef);
            saga.setStatus(SagaStatus.COMPENSATED);
            log.info("Compensation successful: sagaId={}", saga.getId());
        } catch (Exception compensationError) {
            log.error(
                    "CRITICAL: Compensation failed for sagaId={}, manual intervention required: {}",
                    saga.getId(),
                    compensationError.getMessage());
            saga.setStatus(SagaStatus.FAILED);
        }

        saga.setFailureReason(reason);
        transferSagaRepository.save(saga);

        saveFailedEvent(saga, reason);
    }

    @Transactional
    private void failSaga(TransferSaga saga, String reason) {
        saga.setStatus(SagaStatus.FAILED);
        saga.setFailureReason(reason);
        transferSagaRepository.save(saga);

        saveFailedEvent(saga, reason);
    }

    @Transactional
    private void completeSaga(TransferSaga saga, String reference) {
        saga.setStatus(SagaStatus.COMPLETED);
        transferSagaRepository.save(saga);

        String transactionId = UUID.randomUUID().toString();

        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .type(TransactionType.TRANSFER)
                        .status(TransactionStatus.COMPLETED)
                        .sourceAccountId(saga.getSourceAccountId())
                        .targetAccountId(saga.getTargetAccountId())
                        .amount(saga.getAmount())
                        .currency(saga.getCurrency())
                        .description(saga.getDescription())
                        .reference(reference)
                        .idempotencyKey(saga.getIdempotencyKey())
                        .build();
        transactionRepository.save(transaction);

        AccountBalanceResponse sourceAccount =
                accountClient.getAccount(saga.getSourceAccountId());
        BigDecimal sourceBalanceAfter = sourceAccount.getBalance();

        AccountBalanceResponse targetAccount =
                accountClient.getAccount(saga.getTargetAccountId());
        BigDecimal targetBalanceAfter = targetAccount.getBalance();

        LedgerEntry debitEntry =
                LedgerEntry.builder()
                        .transactionId(transactionId)
                        .accountId(saga.getSourceAccountId())
                        .entryType(EntryType.DEBIT)
                        .amount(saga.getAmount())
                        .currency(saga.getCurrency())
                        .balanceAfter(sourceBalanceAfter)
                        .description("Transfer to account " + saga.getTargetAccountId())
                        .build();
        ledgerEntryRepository.save(debitEntry);

        LedgerEntry creditEntry =
                LedgerEntry.builder()
                        .transactionId(transactionId)
                        .accountId(saga.getTargetAccountId())
                        .entryType(EntryType.CREDIT)
                        .amount(saga.getAmount())
                        .currency(saga.getCurrency())
                        .balanceAfter(targetBalanceAfter)
                        .description("Transfer from account " + saga.getSourceAccountId())
                        .build();
        ledgerEntryRepository.save(creditEntry);

        saveCompletedEvent(saga);

        log.info(
                "Transfer saga completed: sagaId={}, transactionId={}",
                saga.getId(),
                transactionId);
    }

    private void saveInitiatedEvent(TransferSaga saga) {
        TransferInitiatedEvent event =
                TransferInitiatedEvent.builder()
                        .eventType("TRANSFER_INITIATED")
                        .aggregateId(saga.getId())
                        .transferId(saga.getId())
                        .idempotencyKey(saga.getIdempotencyKey())
                        .sourceAccountId(saga.getSourceAccountId())
                        .targetAccountId(saga.getTargetAccountId())
                        .amount(saga.getAmount())
                        .currency(saga.getCurrency())
                        .description(saga.getDescription())
                        .build();
        event.initDefaults();
        saveOutboxEvent("Transfer", saga.getId(), "TRANSFER_INITIATED", event);
    }

    private void saveCompletedEvent(TransferSaga saga) {
        TransferCompletedEvent event =
                TransferCompletedEvent.builder()
                        .eventType("TRANSFER_COMPLETED")
                        .aggregateId(saga.getId())
                        .transferId(saga.getId())
                        .sourceAccountId(saga.getSourceAccountId())
                        .targetAccountId(saga.getTargetAccountId())
                        .amount(saga.getAmount())
                        .currency(saga.getCurrency())
                        .build();
        event.initDefaults();
        saveOutboxEvent("Transfer", saga.getId(), "TRANSFER_COMPLETED", event);
    }

    private void saveFailedEvent(TransferSaga saga, String reason) {
        TransferFailedEvent event =
                TransferFailedEvent.builder()
                        .eventType("TRANSFER_FAILED")
                        .aggregateId(saga.getId())
                        .transferId(saga.getId())
                        .sourceAccountId(saga.getSourceAccountId())
                        .targetAccountId(saga.getTargetAccountId())
                        .amount(saga.getAmount())
                        .currency(saga.getCurrency())
                        .reason(reason)
                        .build();
        event.initDefaults();
        saveOutboxEvent("Transfer", saga.getId(), "TRANSFER_FAILED", event);
    }

    private void saveOutboxEvent(
            String aggregateType, String aggregateId, String eventType, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent =
                    OutboxEvent.builder()
                            .aggregateType(aggregateType)
                            .aggregateId(aggregateId)
                            .eventType(eventType)
                            .payload(payload)
                            .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to serialize event: " + e.getMessage());
        }
    }
}
