package com.lorettabank.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorettabank.shared.event.DomainEvent;
import com.lorettabank.shared.event.EventTopics;
import com.lorettabank.transaction.entity.OutboxEvent;
import com.lorettabank.transaction.repository.OutboxEventRepository;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Claims a batch with {@code FOR UPDATE SKIP LOCKED}, so several replicas can run this
     * concurrently without publishing the same row twice. A failed send stops the batch to keep
     * per-aggregate ordering; the row is retried on the next tick.
     */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> batch = outboxEventRepository.claimBatch(BATCH_SIZE);
        for (OutboxEvent outboxEvent : batch) {
            try {
                DomainEvent event =
                        objectMapper.readValue(outboxEvent.getPayload(), DomainEvent.class);
                kafkaTemplate
                        .send(EventTopics.TRANSFER_EVENTS, outboxEvent.getAggregateId(), event)
                        .get(10, TimeUnit.SECONDS);
                outboxEvent.setPublished(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error(
                        "Failed to publish outbox event {}: {}",
                        outboxEvent.getId(),
                        e.getMessage());
                return;
            }
        }
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void prune() {
        int pruned = outboxEventRepository.deletePublishedOlderThanDays(7);
        log.info("Pruned {} published outbox events", pruned);
    }
}
