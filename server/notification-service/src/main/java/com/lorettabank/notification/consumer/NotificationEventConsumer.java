package com.lorettabank.notification.consumer;

import com.lorettabank.notification.entity.NotificationChannel;
import com.lorettabank.notification.entity.NotificationType;
import com.lorettabank.notification.service.NotificationService;
import com.lorettabank.shared.event.AccountOpenedEvent;
import com.lorettabank.shared.event.CustomerCreatedEvent;
import com.lorettabank.shared.event.DomainEvent;
import com.lorettabank.shared.event.EventTopics;
import com.lorettabank.shared.event.MoneyDepositedEvent;
import com.lorettabank.shared.event.MoneyWithdrawnEvent;
import com.lorettabank.shared.event.TransferCompletedEvent;
import com.lorettabank.shared.event.TransferFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = EventTopics.CUSTOMER_EVENTS)
    public void handleCustomerEvents(DomainEvent event) {
        log.info("Received customer event: {}", event.getEventType());
        if (event instanceof CustomerCreatedEvent customerEvent) {
            handleCustomerCreated(customerEvent);
        }
    }

    @KafkaListener(topics = EventTopics.ACCOUNT_EVENTS)
    public void handleAccountEvents(DomainEvent event) {
        log.info("Received account event: {}", event.getEventType());
        if (event instanceof AccountOpenedEvent accountEvent) {
            handleAccountOpened(accountEvent);
        }
    }

    @KafkaListener(topics = EventTopics.TRANSACTION_EVENTS)
    public void handleTransactionEvents(DomainEvent event) {
        log.info("Received transaction event: {}", event.getEventType());
        if (event instanceof MoneyDepositedEvent depositEvent) {
            handleMoneyDeposited(depositEvent);
        } else if (event instanceof MoneyWithdrawnEvent withdrawEvent) {
            handleMoneyWithdrawn(withdrawEvent);
        }
    }

    @KafkaListener(topics = EventTopics.TRANSFER_EVENTS)
    public void handleTransferEvents(DomainEvent event) {
        log.info("Received transfer event: {}", event.getEventType());
        if (event instanceof TransferCompletedEvent transferEvent) {
            handleTransferCompleted(transferEvent);
        } else if (event instanceof TransferFailedEvent failedEvent) {
            handleTransferFailed(failedEvent);
        }
    }

    private void handleCustomerCreated(CustomerCreatedEvent event) {
        String subject = "Welcome to Loretta Bank";
        String body =
                String.format(
                        "Dear %s %s, welcome to Loretta Bank! Your account has been created successfully.",
                        event.getFirstName(), event.getLastName());

        notificationService.createAndSendNotification(
                event.getCustomerId(),
                NotificationType.CUSTOMER_CREATED,
                NotificationChannel.EMAIL,
                event.getEmail(),
                subject,
                body,
                event.getEventType(),
                event.getEventId());
    }

    private void handleAccountOpened(AccountOpenedEvent event) {
        String subject = "New Account Opened";
        String body =
                String.format(
                        "Your new %s account %s has been opened with an initial balance of %s %s.",
                        event.getAccountType(),
                        event.getAccountNumber(),
                        event.getInitialBalance(),
                        event.getCurrency());

        notificationService.createAndSendNotification(
                event.getCustomerId(),
                NotificationType.ACCOUNT_OPENED,
                NotificationChannel.EMAIL,
                String.valueOf(event.getCustomerId()),
                subject,
                body,
                event.getEventType(),
                event.getEventId());
    }

    private void handleMoneyDeposited(MoneyDepositedEvent event) {
        String subject = "Deposit Received";
        String body =
                String.format(
                        "Deposit of %s %s to account %s. New balance: %s %s.",
                        event.getAmount(),
                        event.getCurrency(),
                        event.getAccountNumber(),
                        event.getNewBalance(),
                        event.getCurrency());

        notificationService.createAndSendNotification(
                null,
                NotificationType.DEPOSIT,
                NotificationChannel.EMAIL,
                String.valueOf(event.getAccountId()),
                subject,
                body,
                event.getEventType(),
                event.getEventId());
    }

    private void handleMoneyWithdrawn(MoneyWithdrawnEvent event) {
        String subject = "Withdrawal Processed";
        String body =
                String.format(
                        "Withdrawal of %s %s from account %s. New balance: %s %s.",
                        event.getAmount(),
                        event.getCurrency(),
                        event.getAccountNumber(),
                        event.getNewBalance(),
                        event.getCurrency());

        notificationService.createAndSendNotification(
                null,
                NotificationType.WITHDRAWAL,
                NotificationChannel.EMAIL,
                String.valueOf(event.getAccountId()),
                subject,
                body,
                event.getEventType(),
                event.getEventId());
    }

    private void handleTransferCompleted(TransferCompletedEvent event) {
        String subject = "Transfer Completed";
        String body =
                String.format(
                        "Transfer of %s %s completed successfully. Transfer ID: %s.",
                        event.getAmount(), event.getCurrency(), event.getTransferId());

        notificationService.createAndSendNotification(
                null,
                NotificationType.TRANSFER_COMPLETED,
                NotificationChannel.EMAIL,
                String.valueOf(event.getSourceAccountId()),
                subject,
                body,
                event.getEventType(),
                event.getEventId());
    }

    private void handleTransferFailed(TransferFailedEvent event) {
        String subject = "Transfer Failed";
        String body =
                String.format(
                        "Transfer of %s %s failed: %s. Transfer ID: %s.",
                        event.getAmount(),
                        event.getCurrency(),
                        event.getReason(),
                        event.getTransferId());

        notificationService.createAndSendNotification(
                null,
                NotificationType.TRANSFER_FAILED,
                NotificationChannel.EMAIL,
                String.valueOf(event.getSourceAccountId()),
                subject,
                body,
                event.getEventType(),
                event.getEventId());
    }
}
