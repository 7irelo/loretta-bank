package com.lorettabank.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {
    private int id;
    private String userId;
    private int accountId;
    private String loanType;
    private double amount;
    private double interestRate;
    private int term;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
