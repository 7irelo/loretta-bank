package com.lorettabank.transaction.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.lorettabank.transaction.entity.EntryType;
import com.lorettabank.transaction.entity.LedgerEntry;
import com.lorettabank.transaction.entity.SagaStatus;
import com.lorettabank.transaction.entity.Transaction;
import com.lorettabank.transaction.entity.TransactionStatus;
import com.lorettabank.transaction.entity.TransactionType;
import com.lorettabank.transaction.entity.TransferSaga;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(properties = "eureka.client.enabled=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("loretta_transaction_test")
                    .withUsername("loretta")
                    .withPassword("loretta_secret");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private TransferSagaRepository transferSagaRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    void shouldPersistAndLoadSagaByIdempotencyKey() {
        TransferSaga saga =
                TransferSaga.builder()
                        .id(UUID.randomUUID().toString())
                        .idempotencyKey("idem-transfer-001")
                        .sourceAccountId(101L)
                        .targetAccountId(202L)
                        .amount(new BigDecimal("1200.0000"))
                        .currency("ZAR")
                        .description("Integration transfer")
                        .status(SagaStatus.INITIATED)
                        .build();

        transferSagaRepository.save(saga);

        assertThat(transferSagaRepository.findByIdempotencyKey("idem-transfer-001"))
                .isPresent()
                .get()
                .extracting(TransferSaga::getSourceAccountId)
                .isEqualTo(101L);
    }

    @Test
    void shouldPersistTransactionAndLedgerEntries() {
        String transactionId = UUID.randomUUID().toString();

        Transaction transaction =
                Transaction.builder()
                        .id(transactionId)
                        .type(TransactionType.DEPOSIT)
                        .status(TransactionStatus.COMPLETED)
                        .targetAccountId(5001L)
                        .amount(new BigDecimal("500.0000"))
                        .currency("ZAR")
                        .description("Salary")
                        .reference("DEP-IT-001")
                        .idempotencyKey("idem-deposit-001")
                        .build();
        transactionRepository.save(transaction);

        LedgerEntry ledgerEntry =
                LedgerEntry.builder()
                        .transactionId(transactionId)
                        .accountId(5001L)
                        .entryType(EntryType.CREDIT)
                        .amount(new BigDecimal("500.0000"))
                        .currency("ZAR")
                        .balanceAfter(new BigDecimal("1500.0000"))
                        .description("Salary credit")
                        .build();
        ledgerEntryRepository.save(ledgerEntry);

        assertThat(transactionRepository.findByIdempotencyKey("idem-deposit-001")).isPresent();
        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(transactionId);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getAccountId()).isEqualTo(5001L);
    }
}
