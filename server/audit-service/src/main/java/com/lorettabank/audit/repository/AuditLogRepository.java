package com.lorettabank.audit.repository;

import com.lorettabank.audit.entity.AuditLogEntity;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    boolean existsByEventId(String eventId);

    Page<AuditLogEntity> findByEventTypeOrderByOccurredAtDesc(String eventType, Pageable pageable);

    Page<AuditLogEntity> findByOccurredAtBetweenOrderByOccurredAtDesc(
            Instant from, Instant to, Pageable pageable);

    Page<AuditLogEntity> findByEventTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
            String eventType, Instant from, Instant to, Pageable pageable);

    Page<AuditLogEntity> findByAggregateTypeAndAggregateIdOrderByOccurredAtDesc(
            String aggregateType, String aggregateId, Pageable pageable);
}
