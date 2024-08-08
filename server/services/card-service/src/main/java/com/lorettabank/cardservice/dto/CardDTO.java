package com.lorettabank.cardservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDTO {
    private int id;
    private String userId;
    private int accountId;
    private String cardNumber;
    private Date expiryDate;
    private String cvv;
    private double creditLimit;
    private double balance;
}
