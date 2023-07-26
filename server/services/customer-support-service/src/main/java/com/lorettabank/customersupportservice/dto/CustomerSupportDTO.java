package com.lorettabank.customersupportservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Node("CustomerSupport")
public class CustomerSupportDTO {
    @Id
    @GeneratedValue
    private Long id;

    @Property("query")
    private String query;

    @Property("response")
    private String response;

    @Property("status")
    private String status;

    @Property("created_at")
    private LocalDateTime createdAt;

    @Property("updated_at")
    private LocalDateTime updatedAt;

    @Relationship(type = "RAISED_BY", direction = Relationship.Direction.OUTGOING)
    private UserDTO user;
}
