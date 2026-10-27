package com.lorettabank.transaction.repository;

import com.lorettabank.transaction.entity.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository
        extends JpaRepository<Transaction, String>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /** Outgoing money a customer has committed to (pending or completed) since a point in time. */
    @Query(
            value =
                    "SELECT COALESCE(SUM(amount), 0) FROM transactions"
                            + " WHERE source_customer_id = :customerId"
                            + " AND type IN ('TRANSFER', 'WITHDRAWAL')"
                            + " AND status IN ('PENDING', 'COMPLETED')"
                            + " AND created_at >= :since",
            nativeQuery = true)
    BigDecimal sumOutgoingSince(
            @Param("customerId") Long customerId, @Param("since") Instant since);

    @Query(
            value =
                    "SELECT id FROM transactions WHERE status = 'PENDING'"
                            + " AND updated_at < NOW() - INTERVAL '10 seconds'"
                            + " ORDER BY updated_at LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<String> claimStalePending(@Param("limit") int limit);
}
