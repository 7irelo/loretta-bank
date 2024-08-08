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
@Node("Card")
public class CardDTO {
    @Id
    @GeneratedValue
    private Long id;

    @Property("card_number")
    private String cardNumber;

    @Property("expiry_date")
    private LocalDate expiryDate;

    @Property("cvv")
    private String cvv;

    @Property("credit_limit")
    private double creditLimit;

    @Property("balance")
    private double balance;

    @Property("created_at")
    private LocalDateTime createdAt;

    @Property("updated_at")
    private LocalDateTime updatedAt;

    @Relationship(type = "OWNED_BY", direction = Relationship.Direction.INCOMING)
    private UserDTO user;

    @Relationship(type = "LINKED_TO", direction = Relationship.Direction.OUTGOING)
    private AccountDTO account;
}
