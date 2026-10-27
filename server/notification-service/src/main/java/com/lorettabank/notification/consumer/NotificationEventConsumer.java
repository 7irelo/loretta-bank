package com.lorettabank.notification.consumer;

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
import java.math.BigDecimal;
import java.math.RoundingMode;
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
        if (event instanceof CustomerCreatedEvent e && e.getUserId() != null) {
            notificationService.rememberCustomer(
                    e.getCustomerId(), e.getUserId(), e.getEmail(), e.getFirstName());
            notificationService.notifyCustomer(
                    e.getCustomerId(),
                    NotificationType.CUSTOMER_CREATED,
                    "Welcome to Loretta Bank",
                    "Hi "
                            + e.getFirstName()
                            + ", your profile is set up. We'll let you know once your identity"
                            + " has been verified.",
                    e.getEventType(),
                    e.getEventId());
        }
    }

    @KafkaListener(topics = EventTopics.ACCOUNT_EVENTS)
    public void handleAccountEvents(DomainEvent event) {
        if (event instanceof AccountOpenedEvent e) {
            notificationService.notifyCustomer(
                    e.getCustomerId(),
                    NotificationType.ACCOUNT_OPENED,
                    "New account opened",
                    "Your new "
                            + e.getAccountType().toLowerCase()
                            + " account "
                            + mask(e.getAccountNumber())
                            + " is ready.",
                    e.getEventType(),
                    e.getEventId());
        } else if (event instanceof MoneyDepositedEvent e) {
            notificationService.notifyCustomer(
                    e.getCustomerId(),
                    NotificationType.DEPOSIT,
                    "Deposit received",
                    money(e.getAmount(), e.getCurrency())
                            + " was deposited into "
                            + mask(e.getAccountNumber())
                            + ".",
                    e.getEventType(),
                    e.getEventId());
        } else if (event instanceof MoneyWithdrawnEvent e) {
            notificationService.notifyCustomer(
                    e.getCustomerId(),
                    NotificationType.WITHDRAWAL,
                    "Withdrawal processed",
                    money(e.getAmount(), e.getCurrency())
                            + " was withdrawn from "
                            + mask(e.getAccountNumber())
                            + ".",
                    e.getEventType(),
                    e.getEventId());
        }
    }

    @KafkaListener(topics = EventTopics.TRANSFER_EVENTS)
    public void handleTransferEvents(DomainEvent event) {
        if (event instanceof TransferCompletedEvent e) {
            notificationService.notifyCustomer(
                    e.getSourceCustomerId(),
                    NotificationType.TRANSFER_COMPLETED,
                    "Payment sent",
                    "Your payment of " + money(e.getAmount(), e.getCurrency()) + " was successful.",
                    e.getEventType(),
                    e.getEventId());
            if (e.getTargetCustomerId() != null
                    && !e.getTargetCustomerId().equals(e.getSourceCustomerId())) {
                notificationService.notifyCustomer(
                        e.getTargetCustomerId(),
                        NotificationType.TRANSFER_COMPLETED,
                        "Payment received",
                        "You received " + money(e.getAmount(), e.getCurrency()) + ".",
                        e.getEventType(),
                        e.getEventId());
            }
        } else if (event instanceof TransferFailedEvent e) {
            notificationService.notifyCustomer(
                    e.getSourceCustomerId(),
                    NotificationType.TRANSFER_FAILED,
                    "Payment failed",
                    "Your payment of "
                            + money(e.getAmount(), e.getCurrency())
                            + " could not be completed: "
                            + e.getReason(),
                    e.getEventType(),
                    e.getEventId());
        }
    }

    private static String money(BigDecimal amount, String currency) {
        String symbol = "ZAR".equals(currency) ? "R" : currency + " ";
        return symbol + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "your account";
        }
        return "account ending " + accountNumber.substring(accountNumber.length() - 4);
    }
}
