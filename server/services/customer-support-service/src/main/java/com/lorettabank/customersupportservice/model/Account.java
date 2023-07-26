package com.lorettabank.customersupportservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Node("Account")
public class Account {
    @Id
    @GeneratedValue
    private Long id;

    @Property("account_number")
    private String accountNumber;

    @Property("name")
    private String name;

    @Property("account_type")
    private String accountType;

    @Property("available_balance")
    private double availableBalance;

    @Property("latest_balance")
    private double latestBalance;

    @Property("account_status")
    private String accountStatus;

    @Property("created_at")
    private LocalDateTime createdAt;

    @Property("updated_at")
    private LocalDateTime updatedAt;

    @Relationship(type = "OWNED_BY", direction = Relationship.Direction.INCOMING)
    private User user;
}
