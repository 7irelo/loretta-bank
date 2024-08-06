package com.lorettabank.customersupportservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Node("Loan")
public class LoanDTO {
    @Id
    @GeneratedValue
    private Long id;

    @Property("loan_type")
    private String loanType;

    @Property("amount")
    private double amount;

    @Property("interest_rate")
    private double interestRate;

    @Property("term")
    private int term;

    @Property("start_date")
    private LocalDate startDate;

    @Property("end_date")
    private LocalDate endDate;

    @Property("created_at")
    private LocalDateTime createdAt;

    @Property("updated_at")
    private LocalDateTime updatedAt;

    @Relationship(type = "OWNED_BY", direction = Relationship.Direction.INCOMING)
    private UserDTO user;

    @Relationship(type = "LINKED_TO", direction = Relationship.Direction.OUTGOING)
    private AccountDTO account;
}
