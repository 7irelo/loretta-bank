package com.lorettabank.audit.consumer;

import com.lorettabank.audit.service.AuditService;
import com.lorettabank.shared.event.DomainEvent;
import com.lorettabank.shared.event.EventTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

    private final AuditService auditService;

    @KafkaListener(
            topics = {
                EventTopics.CUSTOMER_EVENTS,
                EventTopics.ACCOUNT_EVENTS,
                EventTopics.TRANSACTION_EVENTS,
                EventTopics.TRANSFER_EVENTS
            })
    public void consume(
            DomainEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String sourceTopic) {
        log.debug("Received audit event {} from topic {}", event.getEventType(), sourceTopic);
        auditService.recordEvent(event, sourceTopic);
    }
}
