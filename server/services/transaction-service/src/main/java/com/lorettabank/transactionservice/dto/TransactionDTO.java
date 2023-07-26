package com.lorettabank.transactionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private int id;
    private int accountId;
    private String transactionType;
    private double amount;
    private String description;
    private String journalType;
    private String date;
}
