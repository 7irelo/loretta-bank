package com.lorettabank.loanservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanDTO {
    private int id;
    private int userId;
    private int accountId;
    private String loanType;
    private double amount;
    private double interestRate;
    private int term;
    private String startDate;
    private String endDate;
    private String status;
    private String createdAt;
    private String updatedAt;
}
