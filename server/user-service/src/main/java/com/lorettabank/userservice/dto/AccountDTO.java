package com.lorettabank.userservice.dto;

import lombok.Data;

@Data
public class AccountDTO {
    private String name;
    private String accountNumber;
    private double availableBalance;
}
