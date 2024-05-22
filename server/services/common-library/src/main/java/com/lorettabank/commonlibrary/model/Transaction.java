package com.lorettabank.commonlibrary.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "account_id", nullable = false)
    private int accountId;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private LocalDateTime date;

    private String description;

    private String journalType;
}
