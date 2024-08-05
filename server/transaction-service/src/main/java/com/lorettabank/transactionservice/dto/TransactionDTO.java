package com.lorettabank.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private int id;
    private int accountId;
    private String type;
    private double amount;
    private String description;
    private String journalType;
    private String date;
    private AccountDTO account;
}
