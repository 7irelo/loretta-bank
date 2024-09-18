package com.lorettabank.account.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorettabank.account.dto.AccountResponse;
import com.lorettabank.account.dto.CreateAccountRequest;
import com.lorettabank.account.dto.DepositRequest;
import com.lorettabank.account.dto.WithdrawRequest;
import com.lorettabank.account.entity.AccountEntity;
import com.lorettabank.account.entity.AccountStatus;
import com.lorettabank.account.entity.OutboxEvent;
import com.lorettabank.account.mapper.AccountMapper;
import com.lorettabank.account.repository.AccountRepository;
import com.lorettabank.account.repository.OutboxEventRepository;
import com.lorettabank.shared.event.AccountOpenedEvent;
import com.lorettabank.shared.event.MoneyDepositedEvent;
import com.lorettabank.shared.event.MoneyWithdrawnEvent;
import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ACCOUNT_NUMBER_PREFIX = "LOR";

    private final AccountRepository accountRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountMapper accountMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        AccountEntity entity = accountMapper.toEntity(request);
        entity.setAccountNumber(generateUniqueAccountNumber());

        BigDecimal initialDeposit = request.getInitialDeposit();
        if (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
            entity.setBalance(initialDeposit);
        } else {
            entity.setBalance(BigDecimal.ZERO);
        }

        AccountEntity saved = accountRepository.save(entity);
        log.info(
                "Created account {} ({}) for customer {}",
                saved.getAccountNumber(),
                saved.getAccountType(),
                saved.getCustomerId());

        AccountOpenedEvent event =
                AccountOpenedEvent.builder()
                        .accountId(saved.getId())
                        .customerId(saved.getCustomerId())
                        .accountNumber(saved.getAccountNumber())
                        .accountType(saved.getAccountType().name())
                        .currency(saved.getCurrency())
                        .initialBalance(saved.getBalance())
                        .eventType("ACCOUNT_OPENED")
                        .aggregateId(String.valueOf(saved.getId()))
                        .build();
        event.initDefaults();

        saveOutboxEvent("Account", String.valueOf(saved.getId()), "ACCOUNT_OPENED", event);

        return accountMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {
        AccountEntity entity =
                accountRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Account not found with ID: " + id));
        return accountMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        AccountEntity entity =
                accountRepository
                        .findByAccountNumber(accountNumber)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Account not found with number: " + accountNumber));
        return accountMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomer(Long customerId) {
        return accountRepository.findByCustomerId(customerId).stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @Transactional
    public AccountResponse deposit(Long accountId, DepositRequest request) {
        AccountEntity account =
                accountRepository
                        .findById(accountId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Account not found with ID: " + accountId));

        validateAccountActive(account);

        account.setBalance(account.getBalance().add(request.getAmount()));
        AccountEntity saved = accountRepository.save(account);

        log.info(
                "Deposited {} {} into account {} (ref: {})",
                request.getAmount(),
                saved.getCurrency(),
                saved.getAccountNumber(),
                request.getReference());

        MoneyDepositedEvent event =
                MoneyDepositedEvent.builder()
                        .accountId(saved.getId())
                        .accountNumber(saved.getAccountNumber())
                        .amount(request.getAmount())
                        .currency(saved.getCurrency())
                        .newBalance(saved.getBalance())
                        .reference(request.getReference())
                        .eventType("MONEY_DEPOSITED")
                        .aggregateId(String.valueOf(saved.getId()))
                        .build();
        event.initDefaults();

        saveOutboxEvent("Account", String.valueOf(saved.getId()), "MONEY_DEPOSITED", event);

        return accountMapper.toResponse(saved);
    }

    @Transactional
    public AccountResponse withdraw(Long accountId, WithdrawRequest request) {
        AccountEntity account =
                accountRepository
                        .findById(accountId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Account not found with ID: " + accountId));

        validateAccountActive(account);
        validateSufficientBalance(account, request.getAmount());

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        AccountEntity saved = accountRepository.save(account);

        log.info(
                "Withdrew {} {} from account {} (ref: {})",
                request.getAmount(),
                saved.getCurrency(),
                saved.getAccountNumber(),
                request.getReference());

        MoneyWithdrawnEvent event =
                MoneyWithdrawnEvent.builder()
                        .accountId(saved.getId())
                        .accountNumber(saved.getAccountNumber())
                        .amount(request.getAmount())
                        .currency(saved.getCurrency())
                        .newBalance(saved.getBalance())
                        .reference(request.getReference())
                        .eventType("MONEY_WITHDRAWN")
                        .aggregateId(String.valueOf(saved.getId()))
                        .build();
        event.initDefaults();

        saveOutboxEvent("Account", String.valueOf(saved.getId()), "MONEY_WITHDRAWN", event);

        return accountMapper.toResponse(saved);
    }

    @Transactional
    public AccountResponse creditAccount(Long accountId, BigDecimal amount, String reference) {
        AccountEntity account =
                accountRepository
                        .findById(accountId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Account not found with ID: " + accountId));

        validateAccountActive(account);

        account.setBalance(account.getBalance().add(amount));
        AccountEntity saved = accountRepository.save(account);

        log.info(
                "Credited {} {} to account {} (ref: {})",
                amount,
                saved.getCurrency(),
                saved.getAccountNumber(),
                reference);

        MoneyDepositedEvent event =
                MoneyDepositedEvent.builder()
                        .accountId(saved.getId())
                        .accountNumber(saved.getAccountNumber())
                        .amount(amount)
                        .currency(saved.getCurrency())
                        .newBalance(saved.getBalance())
                        .reference(reference)
                        .eventType("MONEY_DEPOSITED")
                        .aggregateId(String.valueOf(saved.getId()))
                        .build();
        event.initDefaults();

        saveOutboxEvent("Account", String.valueOf(saved.getId()), "MONEY_DEPOSITED", event);

        return accountMapper.toResponse(saved);
    }

    @Transactional
    public AccountResponse debitAccount(Long accountId, BigDecimal amount, String reference) {
        AccountEntity account =
                accountRepository
                        .findById(accountId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Account not found with ID: " + accountId));

        validateAccountActive(account);
        validateSufficientBalance(account, amount);

        account.setBalance(account.getBalance().subtract(amount));
        AccountEntity saved = accountRepository.save(account);

        log.info(
                "Debited {} {} from account {} (ref: {})",
                amount,
                saved.getCurrency(),
                saved.getAccountNumber(),
                reference);

        MoneyWithdrawnEvent event =
                MoneyWithdrawnEvent.builder()
                        .accountId(saved.getId())
                        .accountNumber(saved.getAccountNumber())
                        .amount(amount)
                        .currency(saved.getCurrency())
                        .newBalance(saved.getBalance())
                        .reference(reference)
                        .eventType("MONEY_WITHDRAWN")
                        .aggregateId(String.valueOf(saved.getId()))
                        .build();
        event.initDefaults();

        saveOutboxEvent("Account", String.valueOf(saved.getId()), "MONEY_WITHDRAWN", event);

        return accountMapper.toResponse(saved);
    }

    @Transactional
    public AccountResponse freezeAccount(Long id) {
        AccountEntity account =
                accountRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Account not found with ID: " + id));

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BusinessException("Cannot freeze a closed account");
        }

        account.setStatus(AccountStatus.FROZEN);
        AccountEntity saved = accountRepository.save(account);
        log.info("Frozen account {}", saved.getAccountNumber());
        return accountMapper.toResponse(saved);
    }

    @Transactional
    public AccountResponse closeAccount(Long id) {
        AccountEntity account =
                accountRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Account not found with ID: " + id));

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(
                    "Cannot close account with non-zero balance. Current balance: "
                            + account.getBalance());
        }

        account.setStatus(AccountStatus.CLOSED);
        AccountEntity saved = accountRepository.save(account);
        log.info("Closed account {}", saved.getAccountNumber());
        return accountMapper.toResponse(saved);
    }

    private void validateAccountActive(AccountEntity account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(
                    "Account " + account.getAccountNumber() + " is not active. Status: "
                            + account.getStatus());
        }
    }

    private void validateSufficientBalance(AccountEntity account, BigDecimal amount) {
        BigDecimal availableBalance = account.getBalance();
        if (account.isOverdraftEnabled()) {
            availableBalance = availableBalance.add(account.getOverdraftLimit());
        }

        if (availableBalance.compareTo(amount) < 0) {
            throw new BusinessException(
                    "Insufficient balance. Available: " + availableBalance
                            + ", requested: " + amount);
        }
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            accountNumber =
                    ACCOUNT_NUMBER_PREFIX + String.format("%010d", RANDOM.nextLong(10_000_000_000L));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
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
            log.error("Failed to serialize outbox event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }
}
