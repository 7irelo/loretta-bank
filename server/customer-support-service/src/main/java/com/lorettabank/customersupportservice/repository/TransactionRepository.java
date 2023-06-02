package com.lorettabank.customersupportservice.repository;

import com.lorettabank.customersupportservice.dto.TransactionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class TransactionRepository {

    @Autowired
    private Neo4jClient neo4jClient;

    public List<TransactionDTO> findLatest15ByUserId(String userId) {
        String query = "MATCH (t:Transaction)-[:BELONGS_TO]->(a:Account)-[:OWNED_BY]->(u:User {id: $userId}) " +
                       "RETURN t ORDER BY t.date DESC LIMIT 15";
        return neo4jClient.query(query)
                .bind(userId).to("userId")
                .fetchAs(TransactionDTO.class)
                .mappedBy((typeSystem, record) -> new TransactionDTO(
                        record.get("id").asLong(),
                        record.get("transaction_type").asString(),
                        record.get("amount").asDouble(),
                        record.get("date").asLocalDateTime(),
                        record.get("description").asString(),
                        record.get("journal_type").asString(),
                        null
                ))
                .all()
                .stream()
                .collect(Collectors.toList());
    }
}
