package com.lorettabank.customersupportservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Node("Account")
public class AccountDTO {
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
    private UserDTO user;
}
