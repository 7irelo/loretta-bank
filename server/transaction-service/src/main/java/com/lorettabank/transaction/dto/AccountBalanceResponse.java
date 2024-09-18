package com.lorettabank.transaction.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceResponse {

    private Long id;
    private String accountNumber;
    private Long customerId;
    private BigDecimal balance;
    private String currency;
    private String status;
}
