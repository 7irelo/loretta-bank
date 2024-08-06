package com.lorettabank.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    private int id;
    private int accountId;
    private String transactionType;
    private double amount;
    private LocalDateTime date;
    private String description;
    private String journalType;
}
