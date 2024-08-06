package com.lorettabank.customersupportservice.repository;

import com.lorettabank.userservice.dto.CustomerSupportDTO;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerSupportRepository extends Neo4jRepository<CustomerSupportDTO, Long> {
    
    Optional<CustomerSupportDTO> findByIdAndUserId(Long id, Long userId);
    
    void deleteByIdAndUserId(Long id, Long userId);
}
