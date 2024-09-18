package com.lorettabank.account.dto;

import com.lorettabank.account.entity.AccountStatus;
import com.lorettabank.account.entity.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private Long customerId;
    private AccountType accountType;
    private String currency;
    private BigDecimal balance;
    private boolean overdraftEnabled;
    private BigDecimal overdraftLimit;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
