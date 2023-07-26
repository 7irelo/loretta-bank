package com.lorettabank.customersupportservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Node("User")
public class User {
    @Id
    private String id;

    @Property("first_name")
    private String firstName;

    @Property("last_name")
    private String lastName;

    @Property("address")
    private String address;

    @Property("date_of_birth")
    private LocalDate dateOfBirth;

    @Property("occupation")
    private String occupation;

    @Property("phone")
    private String phone;

    @Property("email")
    private String email;

    @Property("username")
    private String username;

    @Property("password")
    private String password;

    @Property("created_at")
    private LocalDateTime createdAt;

    @Property("updated_at")
    private LocalDateTime updatedAt;

    @Relationship(type = "HAS_TRANSACTION", direction = Relationship.Direction.OUTGOING)
    private List<Transaction> latestTransactions;
}
