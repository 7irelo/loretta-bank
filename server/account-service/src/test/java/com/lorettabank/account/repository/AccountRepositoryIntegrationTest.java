package com.lorettabank.account.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.lorettabank.account.entity.AccountEntity;
import com.lorettabank.account.entity.AccountStatus;
import com.lorettabank.account.entity.AccountType;
import java.math.BigDecimal;
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
class AccountRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("loretta_account_test")
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
    private AccountRepository accountRepository;

    @Test
    void shouldPersistAndLoadAccountByAccountNumber() {
        AccountEntity entity =
                AccountEntity.builder()
                        .accountNumber("LOR0000000001")
                        .customerId(11L)
                        .accountType(AccountType.CHECKING)
                        .currency("ZAR")
                        .balance(new BigDecimal("2500.0000"))
                        .overdraftEnabled(false)
                        .overdraftLimit(BigDecimal.ZERO)
                        .status(AccountStatus.ACTIVE)
                        .build();

        AccountEntity saved = accountRepository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(accountRepository.existsByAccountNumber("LOR0000000001")).isTrue();
        assertThat(accountRepository.findByAccountNumber("LOR0000000001"))
                .isPresent()
                .get()
                .extracting(AccountEntity::getCustomerId)
                .isEqualTo(11L);
    }
}
