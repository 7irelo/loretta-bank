package com.lorettabank.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorettabank.audit.dto.AuditLogResponse;
import com.lorettabank.audit.entity.AuditLogEntity;
import com.lorettabank.audit.repository.AuditLogRepository;
import com.lorettabank.shared.dto.PagedResponse;
import com.lorettabank.shared.event.DomainEvent;
import com.lorettabank.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordEvent(DomainEvent event, String sourceTopic) {
        if (event.getEventId() == null) {
            log.warn("Skipping audit record because eventId is missing for type {}", event.getEventType());
            return;
        }

        if (auditLogRepository.existsByEventId(event.getEventId())) {
            log.debug("Skipping already-recorded event {}", event.getEventId());
            return;
        }

        AuditLogEntity entity =
                AuditLogEntity.builder()
                        .eventId(event.getEventId())
                        .eventType(event.getEventType())
                        .aggregateType(resolveAggregateType(event.getEventType()))
                        .aggregateId(event.getAggregateId())
                        .correlationId(event.getCorrelationId())
                        .occurredAt(event.getOccurredAt() != null ? event.getOccurredAt() : Instant.now())
                        .sourceTopic(sourceTopic)
                        .payload(serializeEvent(event))
                        .build();

        auditLogRepository.save(entity);
        log.info(
                "Stored audit log for event {} (type={}, aggregate={}:{})",
                event.getEventId(),
                event.getEventType(),
                entity.getAggregateType(),
                entity.getAggregateId());
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> search(
            String eventType, Instant from, Instant to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));

        Page<AuditLogEntity> result;
        if (eventType != null && from != null && to != null) {
            result =
                    auditLogRepository.findByEventTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
                            eventType, from, to, pageable);
        } else if (eventType != null) {
            result = auditLogRepository.findByEventTypeOrderByOccurredAtDesc(eventType, pageable);
        } else if (from != null || to != null) {
            Instant effectiveFrom = from != null ? from : Instant.EPOCH;
            Instant effectiveTo = to != null ? to : Instant.now();
            result =
                    auditLogRepository.findByOccurredAtBetweenOrderByOccurredAtDesc(
                            effectiveFrom, effectiveTo, pageable);
        } else {
            result = auditLogRepository.findAll(pageable);
        }

        return PagedResponse.<AuditLogResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getAuditTrailForAggregate(
            String aggregateType, String aggregateId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<AuditLogEntity> result =
                auditLogRepository.findByAggregateTypeAndAggregateIdOrderByOccurredAtDesc(
                        aggregateType.toUpperCase(), aggregateId, pageable);

        return PagedResponse.<AuditLogResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getById(Long id) {
        AuditLogEntity entity =
                auditLogRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Audit log not found with id: " + id));
        return toResponse(entity);
    }

    private AuditLogResponse toResponse(AuditLogEntity entity) {
        return AuditLogResponse.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .eventType(entity.getEventType())
                .aggregateType(entity.getAggregateType())
                .aggregateId(entity.getAggregateId())
                .correlationId(entity.getCorrelationId())
                .occurredAt(entity.getOccurredAt())
                .sourceTopic(entity.getSourceTopic())
                .payload(entity.getPayload())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String serializeEvent(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize audit event", e);
        }
    }

    private String resolveAggregateType(String eventType) {
        if (eventType == null) {
            return "UNKNOWN";
        }
        if (eventType.startsWith("CUSTOMER_")) {
            return "CUSTOMER";
        }
        if (eventType.startsWith("ACCOUNT_")) {
            return "ACCOUNT";
        }
        if (eventType.startsWith("MONEY_")) {
            return "ACCOUNT";
        }
        if (eventType.startsWith("TRANSFER_")) {
            return "TRANSFER";
        }
        return "UNKNOWN";
    }
}
