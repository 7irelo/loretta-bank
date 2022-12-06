package com.lorettabank.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanDTO {
    private int id;
    private int userId;
    private String loanType;
    private double amount;
    private String status;
    private String createdAt;
    private String updatedAt;
    private UserDTO user;
}
