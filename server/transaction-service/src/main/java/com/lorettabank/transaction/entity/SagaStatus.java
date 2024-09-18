package com.lorettabank.transaction.entity;

public enum SagaStatus {
    INITIATED,
    DEBITED,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}
