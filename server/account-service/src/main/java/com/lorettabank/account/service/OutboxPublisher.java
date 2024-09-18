package com.lorettabank.account.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorettabank.account.entity.OutboxEvent;
import com.lorettabank.account.repository.OutboxEventRepository;
import com.lorettabank.shared.event.DomainEvent;
import com.lorettabank.shared.event.EventTopics;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> unpublished = outboxEventRepository.findByPublishedFalse();

        for (OutboxEvent outboxEvent : unpublished) {
            try {
                DomainEvent domainEvent =
                        objectMapper.readValue(outboxEvent.getPayload(), DomainEvent.class);

                String topic = resolveTopicForEventType(outboxEvent.getEventType());

                kafkaTemplate.send(topic, outboxEvent.getAggregateId(), domainEvent).get();

                outboxEvent.setPublished(true);
                outboxEventRepository.save(outboxEvent);

                log.debug(
                        "Published outbox event {} (type: {}) to topic {}",
                        outboxEvent.getId(),
                        outboxEvent.getEventType(),
                        topic);
            } catch (JsonProcessingException e) {
                log.error(
                        "Failed to deserialize outbox event {}: {}",
                        outboxEvent.getId(),
                        e.getMessage(),
                        e);
            } catch (Exception e) {
                log.error(
                        "Failed to publish outbox event {}: {}",
                        outboxEvent.getId(),
                        e.getMessage(),
                        e);
            }
        }

        if (!unpublished.isEmpty()) {
            log.info("Published {} outbox events", unpublished.size());
        }
    }

    private String resolveTopicForEventType(String eventType) {
        return switch (eventType) {
            case "ACCOUNT_OPENED", "MONEY_DEPOSITED", "MONEY_WITHDRAWN" ->
                    EventTopics.ACCOUNT_EVENTS;
            case "TRANSFER_INITIATED", "TRANSFER_COMPLETED", "TRANSFER_FAILED" ->
                    EventTopics.TRANSFER_EVENTS;
            default -> EventTopics.ACCOUNT_EVENTS;
        };
    }
}
