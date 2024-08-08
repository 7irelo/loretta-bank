package com.lorettabank.customersupportservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Node("Card")
public class Card {
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
    private User user;

    @Relationship(type = "LINKED_TO", direction = Relationship.Direction.OUTGOING)
    private Account account;
}
