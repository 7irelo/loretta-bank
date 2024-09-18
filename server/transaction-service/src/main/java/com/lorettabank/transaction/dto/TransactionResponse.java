package com.lorettabank.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private String id;
    private String type;
    private String status;
    private Long sourceAccountId;
    private Long targetAccountId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String reference;
    private String idempotencyKey;
    private Instant createdAt;
    private Instant updatedAt;
    private List<LedgerEntryResponse> ledgerEntries;
}
