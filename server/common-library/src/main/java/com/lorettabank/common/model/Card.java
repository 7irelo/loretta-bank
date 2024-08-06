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
public class Card {
    private int id;
    private String userId;
    private int accountId;
    private String cardNumber;
    private LocalDate expiryDate;
    private String cvv;
    private double creditLimit;
    private double balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
