package com.lorettabank.customer.repository;

import com.lorettabank.customer.entity.CustomerEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    org.springframework.data.domain.Page<CustomerEntity> findByKycStatus(
            com.lorettabank.customer.entity.KycStatus kycStatus,
            org.springframework.data.domain.Pageable pageable);

    Optional<CustomerEntity> findByUserId(Long userId);

    Optional<CustomerEntity> findByEmail(String email);

    boolean existsByUserId(Long userId);
}
