package com.lorettabank.customersupportservice.repository;

import com.lorettabank.customersupportservice.dto.CustomerSupportDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CustomerSupportRepository {

    @Autowired
    private Neo4jClient neo4jClient;

    public Optional<CustomerSupportDTO> findByIdAndUserId(Long id, Long userId) {
        String query = "MATCH (c:CustomerSupport {id: $id})-[:RAISED_BY]->(u:User {id: $userId}) " +
                       "RETURN c";
        return neo4jClient.query(query)
                .bind(id).to("id")
                .bind(userId).to("userId")
                .fetchAs(CustomerSupportDTO.class)
                .mappedBy((typeSystem, record) -> new CustomerSupportDTO(
                        record.get("id").asLong(),
                        record.get("query").asString(),
                        record.get("response").asString(),
                        record.get("status").asString(),
                        record.get("created_at").asLocalDateTime(),
                        record.get("updated_at").asLocalDateTime(),
                        null // Replace with proper user mapping if needed
                ))
                .one();
    }

    public void deleteByIdAndUserId(Long id, Long userId) {
        String query = "MATCH (c:CustomerSupport {id: $id})-[:RAISED_BY]->(u:User {id: $userId}) " +
                       "DELETE c";
        neo4jClient.query(query)
                .bind(id).to("id")
                .bind(userId).to("userId")
                .run();
    }
}
