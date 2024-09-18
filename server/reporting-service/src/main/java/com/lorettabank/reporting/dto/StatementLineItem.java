package com.lorettabank.reporting.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementLineItem {

    private String transactionId;
    private Instant createdAt;
    private String type;
    private String direction;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String reference;
}
