package com.lorettabank.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementResponse {

    private Long id;
    private Long accountId;
    private Long customerId;
    private String accountNumber;
    private String currency;
    private LocalDateTime periodFrom;
    private LocalDateTime periodTo;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private int transactionCount;
    private LocalDateTime generatedAt;
    private List<StatementLineItem> lineItems;
}
