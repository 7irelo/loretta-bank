package com.lorettabank.account.service;

import com.lorettabank.account.dto.AccountResponse;
import com.lorettabank.account.dto.CreateAccountRequest;
import com.lorettabank.account.entity.AccountEntity;
import com.lorettabank.account.entity.AccountStatus;
import com.lorettabank.account.mapper.AccountMapper;
import com.lorettabank.account.repository.AccountRepository;
import com.lorettabank.shared.dto.PagedResponse;
import com.lorettabank.shared.event.AccountOpenedEvent;
import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account lifecycle and metadata. Balances are never changed here; every money movement goes
 * through {@link com.lorettabank.account.ledger.LedgerService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ACCOUNT_NUMBER_PREFIX = "LOR";
    private static final int MAX_ACCOUNTS_PER_CUSTOMER = 5;

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final OutboxWriter outboxWriter;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.findByCustomerId(request.getCustomerId()).size()
                >= MAX_ACCOUNTS_PER_CUSTOMER) {
            throw new BusinessException(
                    "A customer can hold at most " + MAX_ACCOUNTS_PER_CUSTOMER + " accounts");
        }

        AccountEntity entity = accountMapper.toEntity(request);
        entity.setAccountNumber(generateUniqueAccountNumber());
        entity.setBalance(BigDecimal.ZERO);
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
        outboxWriter.write("Account", String.valueOf(saved.getId()), "ACCOUNT_OPENED", event);

        return accountMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {
        return accountMapper.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        AccountEntity entity =
                accountRepository
                        .findByAccountNumber(accountNumber)
                        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return accountMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomer(Long customerId) {
        return accountRepository.findByCustomerId(customerId).stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<AccountResponse> listAccounts(int page, int size) {
        Page<AccountEntity> result =
                accountRepository.findAllByOrderByIdDesc(
                        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return PagedResponse.<AccountResponse>builder()
                .content(result.getContent().stream().map(accountMapper::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional
    public AccountResponse freezeAccount(Long id) {
        AccountEntity account = find(id);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BusinessException("Cannot freeze a closed account");
        }
        account.setStatus(AccountStatus.FROZEN);
        log.info("Froze account {}", account.getAccountNumber());
        return accountMapper.toResponse(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse unfreezeAccount(Long id) {
        AccountEntity account = find(id);
        if (account.getStatus() != AccountStatus.FROZEN) {
            throw new BusinessException("Only frozen accounts can be unfrozen");
        }
        account.setStatus(AccountStatus.ACTIVE);
        log.info("Unfroze account {}", account.getAccountNumber());
        return accountMapper.toResponse(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse closeAccount(Long id) {
        AccountEntity account = find(id);
        if (account.getBalance().signum() != 0) {
            throw new BusinessException("Cannot close an account with a non-zero balance");
        }
        account.setStatus(AccountStatus.CLOSED);
        log.info("Closed account {}", account.getAccountNumber());
        return accountMapper.toResponse(accountRepository.save(account));
    }

    private AccountEntity find(Long id) {
        return accountRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            accountNumber =
                    ACCOUNT_NUMBER_PREFIX
                            + String.format("%010d", RANDOM.nextLong(10_000_000_000L));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}
