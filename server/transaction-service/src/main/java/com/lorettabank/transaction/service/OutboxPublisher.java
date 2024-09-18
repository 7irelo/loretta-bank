package com.lorettabank.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorettabank.shared.event.DomainEvent;
import com.lorettabank.shared.event.EventTopics;
import com.lorettabank.transaction.entity.OutboxEvent;
import com.lorettabank.transaction.repository.OutboxEventRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, DomainEvent> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> unpublished = outboxEventRepository.findByPublishedFalse();
        if (unpublished.isEmpty()) {
            return;
        }

        log.debug("Publishing {} pending outbox events", unpublished.size());

        for (OutboxEvent outboxEvent : unpublished) {
            try {
                DomainEvent domainEvent =
                        objectMapper.readValue(outboxEvent.getPayload(), DomainEvent.class);
                String topic = resolveTopic(outboxEvent.getEventType());

                kafkaTemplate.send(topic, outboxEvent.getAggregateId(), domainEvent).get();

                outboxEvent.setPublished(true);
                outboxEventRepository.save(outboxEvent);

                log.debug(
                        "Published outbox event: id={}, type={}, topic={}",
                        outboxEvent.getId(),
                        outboxEvent.getEventType(),
                        topic);
            } catch (Exception e) {
                log.error(
                        "Error processing outbox event: id={}, error={}",
                        outboxEvent.getId(),
                        e.getMessage(),
                        e);
            }
        }
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case "MONEY_DEPOSITED", "MONEY_WITHDRAWN" -> EventTopics.TRANSACTION_EVENTS;
            case "TRANSFER_INITIATED", "TRANSFER_COMPLETED", "TRANSFER_FAILED" ->
                    EventTopics.TRANSFER_EVENTS;
            default -> EventTopics.TRANSACTION_EVENTS;
        };
    }
}
