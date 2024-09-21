package com.lorettabank.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {
    private int id;
    private String name;
    private String userId;
    private String accountType;
    private double availableBalance;
    private double latestBalance;
    private String accountStatus;
    private String imageUrl;
    private String accountNumber;
}
