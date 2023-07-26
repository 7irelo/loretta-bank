package com.lorettabank.customersupportservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Node("Transaction")
public class TransactionDTO {
    @Id
    @GeneratedValue
    private Long id;

    @Property("transaction_type")
    private String transactionType;

    @Property("amount")
    private double amount;

    @Property("date")
    private LocalDateTime date;

    @Property("description")
    private String description;

    @Property("journal_type")
    private String journalType;

    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.INCOMING)
    private AccountDTO account;
}
