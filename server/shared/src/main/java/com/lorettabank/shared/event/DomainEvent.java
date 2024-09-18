package com.lorettabank.shared.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CustomerCreatedEvent.class, name = "CUSTOMER_CREATED"),
    @JsonSubTypes.Type(value = AccountOpenedEvent.class, name = "ACCOUNT_OPENED"),
    @JsonSubTypes.Type(value = MoneyDepositedEvent.class, name = "MONEY_DEPOSITED"),
    @JsonSubTypes.Type(value = MoneyWithdrawnEvent.class, name = "MONEY_WITHDRAWN"),
    @JsonSubTypes.Type(value = TransferInitiatedEvent.class, name = "TRANSFER_INITIATED"),
    @JsonSubTypes.Type(value = TransferCompletedEvent.class, name = "TRANSFER_COMPLETED"),
    @JsonSubTypes.Type(value = TransferFailedEvent.class, name = "TRANSFER_FAILED"),
})
public abstract class DomainEvent {

    private String eventId;
    private String eventType;
    private Instant occurredAt;
    private String aggregateId;
    private String correlationId;

    public void initDefaults() {
        if (eventId == null) eventId = UUID.randomUUID().toString();
        if (occurredAt == null) occurredAt = Instant.now();
    }
}
