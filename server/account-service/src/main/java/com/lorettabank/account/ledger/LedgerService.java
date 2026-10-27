package com.lorettabank.account.ledger;

import com.lorettabank.account.entity.AccountEntity;
import com.lorettabank.account.entity.AccountStatus;
import com.lorettabank.account.ledger.LedgerDtos.BalanceAt;
import com.lorettabank.account.ledger.LedgerDtos.CashRequest;
import com.lorettabank.account.ledger.LedgerDtos.Entry;
import com.lorettabank.account.ledger.LedgerDtos.JournalResponse;
import com.lorettabank.account.ledger.LedgerDtos.ReconciliationReport;
import com.lorettabank.account.ledger.LedgerDtos.TransferRequest;
import com.lorettabank.account.repository.AccountRepository;
import com.lorettabank.account.service.OutboxWriter;
import com.lorettabank.shared.event.MoneyDepositedEvent;
import com.lorettabank.shared.event.MoneyWithdrawnEvent;
import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.exception.DuplicateResourceException;
import com.lorettabank.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only code path that changes a balance. Each journal is idempotent, locks the accounts it
 * touches, writes balanced postings and the balance projection in one database transaction, and is
 * rejected by the database at commit if its postings do not sum to zero.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final AccountRepository accountRepository;
    private final JournalRepository journalRepository;
    private final PostingRepository postingRepository;
    private final OutboxWriter outboxWriter;

    private record Leg(Long accountId, SystemAccount systemAccount, BigDecimal amount) {}

    @Transactional
    public JournalResponse cashDeposit(CashRequest request, String actor) {
        BigDecimal amount = money(request.amount());
        List<Leg> legs =
                List.of(
                        new Leg(request.accountId(), null, amount),
                        new Leg(null, SystemAccount.CASH_CLEARING, amount.negate()));
        JournalResponse response =
                post(
                        JournalType.CASH_DEPOSIT,
                        request.idempotencyKey(),
                        request.reference(),
                        request.description(),
                        request.currency(),
                        legs,
                        actor);
        if (!response.replayed()) {
            AccountEntity account = accountRepository.findById(request.accountId()).orElseThrow();
            MoneyDepositedEvent event =
                    MoneyDepositedEvent.builder()
                            .eventType("MONEY_DEPOSITED")
                            .aggregateId(String.valueOf(account.getId()))
                            .accountId(account.getId())
                            .customerId(account.getCustomerId())
                            .accountNumber(account.getAccountNumber())
                            .amount(amount)
                            .currency(account.getCurrency())
                            .newBalance(account.getBalance())
                            .reference(request.reference())
                            .build();
            event.initDefaults();
            outboxWriter.write("Account", event.getAggregateId(), event.getEventType(), event);
        }
        return response;
    }

    @Transactional
    public JournalResponse cashWithdrawal(CashRequest request, String actor) {
        BigDecimal amount = money(request.amount());
        List<Leg> legs =
                List.of(
                        new Leg(request.accountId(), null, amount.negate()),
                        new Leg(null, SystemAccount.CASH_CLEARING, amount));
        JournalResponse response =
                post(
                        JournalType.CASH_WITHDRAWAL,
                        request.idempotencyKey(),
                        request.reference(),
                        request.description(),
                        request.currency(),
                        legs,
                        actor);
        if (!response.replayed()) {
            AccountEntity account = accountRepository.findById(request.accountId()).orElseThrow();
            MoneyWithdrawnEvent event =
                    MoneyWithdrawnEvent.builder()
                            .eventType("MONEY_WITHDRAWN")
                            .aggregateId(String.valueOf(account.getId()))
                            .accountId(account.getId())
                            .customerId(account.getCustomerId())
                            .accountNumber(account.getAccountNumber())
                            .amount(amount)
                            .currency(account.getCurrency())
                            .newBalance(account.getBalance())
                            .reference(request.reference())
                            .build();
            event.initDefaults();
            outboxWriter.write("Account", event.getAggregateId(), event.getEventType(), event);
        }
        return response;
    }

    /** Transfer events are emitted by transaction-service, which owns the transfer lifecycle. */
    @Transactional
    public JournalResponse transfer(TransferRequest request, String actor) {
        if (request.sourceAccountId().equals(request.targetAccountId())) {
            throw new BusinessException("Source and target accounts must be different");
        }
        BigDecimal amount = money(request.amount());
        List<Leg> legs =
                List.of(
                        new Leg(request.sourceAccountId(), null, amount.negate()),
                        new Leg(request.targetAccountId(), null, amount));
        return post(
                JournalType.TRANSFER,
                request.idempotencyKey(),
                request.reference(),
                request.description(),
                request.currency(),
                legs,
                actor);
    }

    @Transactional(readOnly = true)
    public Optional<JournalResponse> findByIdempotencyKey(String idempotencyKey) {
        return journalRepository
                .findByIdempotencyKey(idempotencyKey)
                .map(journal -> toResponse(journal, true));
    }

    @Transactional(readOnly = true)
    public BalanceAt balanceAt(Long accountId, Instant at) {
        AccountEntity account =
                accountRepository
                        .findById(accountId)
                        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        BigDecimal balance =
                postingRepository
                        .findFirstByAccountIdAndCreatedAtBeforeOrderByIdDesc(accountId, at)
                        .map(PostingEntity::getBalanceAfter)
                        .orElse(BigDecimal.ZERO);
        return new BalanceAt(accountId, at, balance, account.getCurrency());
    }

    @Transactional(readOnly = true)
    public ReconciliationReport reconcile() {
        BigDecimal total = postingRepository.sumAll();
        List<Long> drift = postingRepository.findAccountsWithDrift();
        boolean healthy = total.signum() == 0 && drift.isEmpty();
        if (!healthy) {
            log.error("LEDGER DRIFT: total={} accountsWithDrift={}", total, drift);
        }
        return new ReconciliationReport(Instant.now(), total, drift, healthy);
    }

    private JournalResponse post(
            JournalType type,
            String idempotencyKey,
            String reference,
            String description,
            String currency,
            List<Leg> legs,
            String actor) {
        Optional<JournalEntity> existing = journalRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            assertSameRequest(existing.get(), type, legs);
            return toResponse(existing.get(), true);
        }

        List<Long> accountIds =
                legs.stream()
                        .map(Leg::accountId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted()
                        .toList();
        Map<Long, AccountEntity> accounts =
                accountRepository.lockAllById(accountIds).stream()
                        .collect(Collectors.toMap(AccountEntity::getId, Function.identity()));
        if (accounts.size() != accountIds.size()) {
            throw new ResourceNotFoundException("One or more accounts do not exist");
        }

        for (Leg leg : legs) {
            if (leg.accountId() == null) {
                continue;
            }
            AccountEntity account = accounts.get(leg.accountId());
            if (account.getStatus() != AccountStatus.ACTIVE) {
                throw new BusinessException(
                        "Account " + account.getAccountNumber() + " is " + account.getStatus());
            }
            if (!account.getCurrency().equals(currency)) {
                throw new BusinessException(
                        "Currency "
                                + currency
                                + " does not match account currency "
                                + account.getCurrency());
            }
            if (leg.amount().signum() < 0) {
                BigDecimal available = account.getBalance();
                if (account.isOverdraftEnabled()) {
                    available = available.add(account.getOverdraftLimit());
                }
                if (available.compareTo(leg.amount().negate()) < 0) {
                    throw new BusinessException("Insufficient funds");
                }
            }
        }

        JournalEntity journal =
                journalRepository.save(
                        JournalEntity.builder()
                                .id(UUID.randomUUID())
                                .idempotencyKey(idempotencyKey)
                                .journalType(type.name())
                                .reference(reference)
                                .description(description)
                                .currency(currency)
                                .createdBy(actor)
                                .build());

        List<PostingEntity> postings = new ArrayList<>();
        for (Leg leg : legs) {
            BigDecimal balanceAfter = null;
            if (leg.accountId() != null) {
                AccountEntity account = accounts.get(leg.accountId());
                account.setBalance(account.getBalance().add(leg.amount()));
                balanceAfter = account.getBalance();
            }
            postings.add(
                    PostingEntity.builder()
                            .journalId(journal.getId())
                            .accountId(leg.accountId())
                            .systemAccount(
                                    leg.systemAccount() == null ? null : leg.systemAccount().name())
                            .amount(leg.amount())
                            .balanceAfter(balanceAfter)
                            .build());
        }
        accountRepository.saveAll(accounts.values());
        postingRepository.saveAll(postings);

        log.info("Posted journal {} type={} ref={} by {}", journal.getId(), type, reference, actor);
        return new JournalResponse(
                journal.getId(),
                journal.getJournalType(),
                journal.getReference(),
                journal.getCurrency(),
                journal.getCreatedAt(),
                false,
                postings.stream().map(LedgerService::toEntry).toList());
    }

    private void assertSameRequest(JournalEntity journal, JournalType type, List<Leg> legs) {
        List<PostingEntity> stored = postingRepository.findByJournalIdOrderByIdAsc(journal.getId());
        boolean same =
                journal.getJournalType().equals(type.name())
                        && stored.size() == legs.size()
                        && IntStream.range(0, legs.size())
                                .allMatch(
                                        i ->
                                                Objects.equals(
                                                                stored.get(i).getAccountId(),
                                                                legs.get(i).accountId())
                                                        && stored.get(i)
                                                                        .getAmount()
                                                                        .compareTo(
                                                                                legs.get(i)
                                                                                        .amount())
                                                                == 0);
        if (!same) {
            throw new DuplicateResourceException(
                    "Idempotency key was already used for a different request");
        }
    }

    private JournalResponse toResponse(JournalEntity journal, boolean replayed) {
        return new JournalResponse(
                journal.getId(),
                journal.getJournalType(),
                journal.getReference(),
                journal.getCurrency(),
                journal.getCreatedAt(),
                replayed,
                postingRepository.findByJournalIdOrderByIdAsc(journal.getId()).stream()
                        .map(LedgerService::toEntry)
                        .toList());
    }

    private static Entry toEntry(PostingEntity posting) {
        return new Entry(
                posting.getAccountId(),
                posting.getSystemAccount(),
                posting.getAmount(),
                posting.getBalanceAfter());
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(4, RoundingMode.UNNECESSARY);
    }
}
