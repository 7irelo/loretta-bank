package com.lorettabank.account.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lorettabank.account.entity.AccountEntity;
import com.lorettabank.account.entity.AccountType;
import com.lorettabank.account.ledger.LedgerDtos.CashRequest;
import com.lorettabank.account.ledger.LedgerDtos.TransferRequest;
import com.lorettabank.account.repository.AccountRepository;
import com.lorettabank.account.service.OutboxWriter;
import com.lorettabank.shared.exception.BusinessException;
import com.lorettabank.shared.exception.DuplicateResourceException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = "eureka.client.enabled=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({LedgerService.class, OutboxWriter.class, JacksonAutoConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LedgerServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("loretta_account");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired private LedgerService ledger;
    @Autowired private AccountRepository accounts;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void depositThenTransferMovesMoneyAndBalances() {
        AccountEntity a = open(1L);
        AccountEntity b = open(2L);
        ledger.cashDeposit(cash(a.getId(), "100.00"), "test");
        ledger.transfer(
                new TransferRequest(
                        key(), a.getId(), b.getId(), new BigDecimal("40.00"), "ZAR", "T", null),
                "test");

        assertThat(balance(a)).isEqualByComparingTo("60");
        assertThat(balance(b)).isEqualByComparingTo("40");
        assertThat(ledger.reconcile().healthy()).isTrue();
    }

    @Test
    void replayingAKeyDoesNotPostTwice() {
        AccountEntity a = open(3L);
        CashRequest request = cash(a.getId(), "25.00");
        var first = ledger.cashDeposit(request, "test");
        var second = ledger.cashDeposit(request, "test");

        assertThat(second.replayed()).isTrue();
        assertThat(second.journalId()).isEqualTo(first.journalId());
        assertThat(balance(a)).isEqualByComparingTo("25");
    }

    @Test
    void reusingAKeyForADifferentRequestIsRejected() {
        AccountEntity a = open(4L);
        String key = key();
        ledger.cashDeposit(
                new CashRequest(key, a.getId(), new BigDecimal("5"), "ZAR", "R", null), "t");
        assertThatThrownBy(
                        () ->
                                ledger.cashDeposit(
                                        new CashRequest(
                                                key,
                                                a.getId(),
                                                new BigDecimal("6"),
                                                "ZAR",
                                                "R",
                                                null),
                                        "t"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void cannotOverdraw() {
        AccountEntity a = open(5L);
        AccountEntity b = open(6L);
        ledger.cashDeposit(cash(a.getId(), "10.00"), "test");
        assertThatThrownBy(
                        () ->
                                ledger.transfer(
                                        new TransferRequest(
                                                key(),
                                                a.getId(),
                                                b.getId(),
                                                new BigDecimal("10.01"),
                                                "ZAR",
                                                "T",
                                                null),
                                        "test"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient");
        assertThat(balance(a)).isEqualByComparingTo("10");
    }

    @Test
    void concurrentWithdrawalsNeverOverdraw() throws Exception {
        AccountEntity a = open(7L);
        ledger.cashDeposit(cash(a.getId(), "100.00"), "test");

        AtomicInteger succeeded = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(16);
        for (int i = 0; i < 50; i++) {
            pool.submit(
                    () -> {
                        try {
                            ledger.cashWithdrawal(cash(a.getId(), "7.00"), "test");
                            succeeded.incrementAndGet();
                        } catch (BusinessException expected) {
                            // insufficient funds
                        }
                    });
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(succeeded.get()).isEqualTo(14);
        assertThat(balance(a)).isEqualByComparingTo("2");
        assertThat(ledger.reconcile().healthy()).isTrue();
    }

    @Test
    void databaseRejectsAnUnbalancedJournal() {
        AccountEntity a = open(8L);
        UUID journal = UUID.randomUUID();
        assertThatThrownBy(
                        () ->
                                new org.springframework.transaction.support.TransactionTemplate(
                                                new org.springframework.jdbc.datasource
                                                        .DataSourceTransactionManager(
                                                        jdbc.getDataSource()))
                                        .executeWithoutResult(
                                                s -> {
                                                    jdbc.update(
                                                            "insert into journals (id, idempotency_key, journal_type,"
                                                                    + " reference, currency, created_by) values (?, ?,"
                                                                    + " 'CASH_DEPOSIT', 'X', 'ZAR', 't')",
                                                            journal,
                                                            key());
                                                    jdbc.update(
                                                            "insert into postings (journal_id, account_id, amount)"
                                                                    + " values (?, ?, 100)",
                                                            journal,
                                                            a.getId());
                                                }))
                .hasMessageContaining("does not balance");
    }

    @Test
    void postingsAreImmutable() {
        AccountEntity a = open(9L);
        ledger.cashDeposit(cash(a.getId(), "1.00"), "test");
        assertThatThrownBy(() -> jdbc.update("update postings set amount = 1000000"))
                .hasMessageContaining("immutable");
    }

    private AccountEntity open(Long customerId) {
        return accounts.save(
                AccountEntity.builder()
                        .accountNumber(
                                "LOR" + String.format("%010d", System.nanoTime() % 10_000_000_000L))
                        .customerId(customerId)
                        .accountType(AccountType.CHECKING)
                        .currency("ZAR")
                        .balance(BigDecimal.ZERO)
                        .build());
    }

    private BigDecimal balance(AccountEntity account) {
        return accounts.findById(account.getId()).orElseThrow().getBalance();
    }

    private static CashRequest cash(Long accountId, String amount) {
        return new CashRequest(key(), accountId, new BigDecimal(amount), "ZAR", "REF", null);
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
