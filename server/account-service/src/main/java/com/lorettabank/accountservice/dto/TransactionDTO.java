package com.lorettabank.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private int id;
    private int accountId;
    private String transactionType;
    private double amount;
    private LocalDateTime date;
    private String description;
    private String journalType;
}
