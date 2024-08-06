package com.lorettabank.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    private int id;
    private String accountNumber;
    private String name;
    private String userId;
    private String accountType;
    private double availableBalance;
    private double latestBalance;
    private String accountStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
