package com.lorettabank.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorettabank.shared.dto.PagedResponse;
import com.lorettabank.shared.event.MoneyDepositedEvent;
import com.lorettabank.shared.event.MoneyWithdrawnEvent;
import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.exception.ResourceNotFoundException;
import com.lorettabank.transaction.client.AccountClient;
import com.lorettabank.transaction.dto.AccountBalanceResponse;
import com.lorettabank.transaction.dto.DepositRequest;
import com.lorettabank.transaction.dto.LedgerEntryResponse;
import com.lorettabank.transaction.dto.TransactionResponse;
import com.lorettabank.transaction.dto.WithdrawRequest;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountClient accountClient;
    private final TransactionMapper transactionMapper;
    private final ObjectMapper objectMapper;

    public TransactionService(
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            OutboxEventRepository outboxEventRepository,
            AccountClient accountClient,
            TransactionMapper transactionMapper,
            ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.accountClient = accountClient;
        this.transactionMapper = transactionMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransactionResponse recordDeposit(DepositRequest request, String idempotencyKey) {
        log.info(
                "Recording deposit: accountId={}, amount={}, idempotencyKey={}",
                request.getAccountId(),
                request.getAmount(),
                idempotencyKey);

        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Returning existing deposit transaction for idempotencyKey={}", idempotencyKey);
            Transaction tx = existing.get();
            List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(tx.getId());
            return transactionMapper.toTransactionResponse(tx, entries);
        }

        String transactionId = UUID.randomUUID().toString();
        String reference = "DEP-" + transactionId.substring(0, 8).toUpperCase();

        accountClient.credit(request.getAccountId(), request.getAmount(), reference);

        AccountBalanceResponse account = accountClient.getAccount(request.getAccountId());
        BigDecimal balanceAfter = account.getBalance();

        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .type(TransactionType.DEPOSIT)
                        .status(TransactionStatus.COMPLETED)
                        .targetAccountId(request.getAccountId())
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .description(request.getDescription())
                        .reference(reference)
                        .idempotencyKey(idempotencyKey)
                        .build();
        transactionRepository.save(transaction);

        LedgerEntry creditEntry =
                LedgerEntry.builder()
                        .transactionId(transactionId)
                        .accountId(request.getAccountId())
                        .entryType(EntryType.CREDIT)
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .balanceAfter(balanceAfter)
                        .description(request.getDescription())
                        .build();
        ledgerEntryRepository.save(creditEntry);

        saveDepositOutboxEvent(transaction, account, balanceAfter);

        log.info("Deposit completed: transactionId={}", transactionId);
        return transactionMapper.toTransactionResponse(transaction, List.of(creditEntry));
    }

    @Transactional
    public TransactionResponse recordWithdrawal(WithdrawRequest request, String idempotencyKey) {
        log.info(
                "Recording withdrawal: accountId={}, amount={}, idempotencyKey={}",
                request.getAccountId(),
                request.getAmount(),
                idempotencyKey);

        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info(
                    "Returning existing withdrawal transaction for idempotencyKey={}",
                    idempotencyKey);
            Transaction tx = existing.get();
            List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(tx.getId());
            return transactionMapper.toTransactionResponse(tx, entries);
        }

        String transactionId = UUID.randomUUID().toString();
        String reference = "WDR-" + transactionId.substring(0, 8).toUpperCase();

        accountClient.debit(request.getAccountId(), request.getAmount(), reference);

        AccountBalanceResponse account = accountClient.getAccount(request.getAccountId());
        BigDecimal balanceAfter = account.getBalance();

        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .type(TransactionType.WITHDRAWAL)
                        .status(TransactionStatus.COMPLETED)
                        .sourceAccountId(request.getAccountId())
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .description(request.getDescription())
                        .reference(reference)
                        .idempotencyKey(idempotencyKey)
                        .build();
        transactionRepository.save(transaction);

        LedgerEntry debitEntry =
                LedgerEntry.builder()
                        .transactionId(transactionId)
                        .accountId(request.getAccountId())
                        .entryType(EntryType.DEBIT)
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .balanceAfter(balanceAfter)
                        .description(request.getDescription())
                        .build();
        ledgerEntryRepository.save(debitEntry);

        saveWithdrawalOutboxEvent(transaction, account, balanceAfter);

        log.info("Withdrawal completed: transactionId={}", transactionId);
        return transactionMapper.toTransactionResponse(transaction, List.of(debitEntry));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String transactionId) {
        Transaction transaction =
                transactionRepository
                        .findById(transactionId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Transaction not found: " + transactionId));
        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(transactionId);
        return transactionMapper.toTransactionResponse(transaction, entries);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getTransactionsForAccount(
            Long accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Transaction> transactionPage =
                transactionRepository.findBySourceAccountIdOrTargetAccountId(
                        accountId, accountId, pageable);
        List<TransactionResponse> content =
                transactionPage.getContent().stream()
                        .map(transactionMapper::toTransactionResponse)
                        .toList();
        return PagedResponse.<TransactionResponse>builder()
                .content(content)
                .page(transactionPage.getNumber())
                .size(transactionPage.getSize())
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .last(transactionPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<LedgerEntryResponse> getLedgerEntriesForAccount(
            Long accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LedgerEntry> entryPage =
                ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
        List<LedgerEntryResponse> content =
                entryPage.getContent().stream()
                        .map(transactionMapper::toLedgerEntryResponse)
                        .toList();
        return PagedResponse.<LedgerEntryResponse>builder()
                .content(content)
                .page(entryPage.getNumber())
                .size(entryPage.getSize())
                .totalElements(entryPage.getTotalElements())
                .totalPages(entryPage.getTotalPages())
                .last(entryPage.isLast())
                .build();
    }

    private void saveDepositOutboxEvent(
            Transaction transaction, AccountBalanceResponse account, BigDecimal balanceAfter) {
        MoneyDepositedEvent event =
                MoneyDepositedEvent.builder()
                        .eventType("MONEY_DEPOSITED")
                        .aggregateId(String.valueOf(transaction.getTargetAccountId()))
                        .accountId(transaction.getTargetAccountId())
                        .accountNumber(account.getAccountNumber())
                        .amount(transaction.getAmount())
                        .currency(transaction.getCurrency())
                        .newBalance(balanceAfter)
                        .reference(transaction.getReference())
                        .build();
        event.initDefaults();
        saveOutboxEvent("Transaction", transaction.getId(), "MONEY_DEPOSITED", event);
    }

    private void saveWithdrawalOutboxEvent(
            Transaction transaction, AccountBalanceResponse account, BigDecimal balanceAfter) {
        MoneyWithdrawnEvent event =
                MoneyWithdrawnEvent.builder()
                        .eventType("MONEY_WITHDRAWN")
                        .aggregateId(String.valueOf(transaction.getSourceAccountId()))
                        .accountId(transaction.getSourceAccountId())
                        .accountNumber(account.getAccountNumber())
                        .amount(transaction.getAmount())
                        .currency(transaction.getCurrency())
                        .newBalance(balanceAfter)
                        .reference(transaction.getReference())
                        .build();
        event.initDefaults();
        saveOutboxEvent("Transaction", transaction.getId(), "MONEY_WITHDRAWN", event);
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
