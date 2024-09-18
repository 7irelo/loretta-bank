package com.lorettabank.customer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorettabank.customer.entity.OutboxEvent;
import com.lorettabank.customer.repository.OutboxEventRepository;
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
        if (unpublished.isEmpty()) {
            return;
        }

        for (OutboxEvent event : unpublished) {
            try {
                DomainEvent domainEvent = objectMapper.readValue(event.getPayload(), DomainEvent.class);
                kafkaTemplate.send(EventTopics.CUSTOMER_EVENTS, event.getAggregateId(), domainEvent).get();

                event.setPublished(true);
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Error publishing customer outbox event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}
