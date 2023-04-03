package com.lorettabank.customersupportservice.repository;

import com.lorettabank.customersupportservice.dto.TransactionDTO;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends Neo4jRepository<TransactionDTO, Long> {

    @Query("MATCH (t:Transaction)-[:BELONGS_TO]->(a:Account)-[:OWNED_BY]->(u:User {id: $userId}) " +
           "RETURN t ORDER BY t.date DESC LIMIT 15")
    List<TransactionDTO> findLatest15ByUserId(String userId);
}
