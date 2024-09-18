package com.lorettabank.transaction.repository;

import com.lorettabank.transaction.entity.TransferSaga;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferSagaRepository extends JpaRepository<TransferSaga, String> {

    Optional<TransferSaga> findByIdempotencyKey(String idempotencyKey);
}
