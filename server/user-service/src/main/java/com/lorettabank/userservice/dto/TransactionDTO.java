package com.lorettabank.userservice.dto;

import lombok.Data;

@Data
public class TransactionDTO {
    private String transactionType;
    private double amount;
}
