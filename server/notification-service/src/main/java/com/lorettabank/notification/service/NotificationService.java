package com.lorettabank.notification.service;

import com.lorettabank.notification.dto.NotificationResponse;
import com.lorettabank.notification.entity.NotificationChannel;
import com.lorettabank.notification.entity.NotificationEntity;
import com.lorettabank.notification.entity.NotificationStatus;
import com.lorettabank.notification.entity.NotificationType;
import com.lorettabank.notification.repository.NotificationRepository;
import com.lorettabank.shared.dto.PagedResponse;
import com.lorettabank.shared.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationEntity createAndSendNotification(
            Long userId,
            NotificationType type,
            NotificationChannel channel,
            String recipient,
            String subject,
            String body,
            String eventType,
            String eventId) {

        NotificationEntity notification =
                NotificationEntity.builder()
                        .userId(userId)
                        .type(type)
                        .channel(channel)
                        .recipient(recipient)
                        .subject(subject)
                        .body(body)
                        .status(NotificationStatus.PENDING)
                        .eventType(eventType)
                        .eventId(eventId)
                        .build();

        notification = notificationRepository.save(notification);
        log.info(
                "Created notification {} of type {} for user {}",
                notification.getId(),
                type,
                userId);

        sendNotification(notification);
        return notification;
    }

    @Transactional
    public void sendNotification(NotificationEntity notification) {
        try {
            log.info(
                    "Sending {} to {}: {}",
                    notification.getChannel().name().toLowerCase(),
                    notification.getRecipient(),
                    notification.getSubject());

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("Notification {} sent successfully", notification.getId());
        } catch (Exception e) {
            log.error("Failed to send notification {}: {}", notification.getId(), e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        Page<NotificationEntity> page = notificationRepository.findByUserId(userId, pageable);
        return PagedResponse.<NotificationResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotification(Long id) {
        NotificationEntity entity =
                notificationRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Notification not found with ID: " + id));
        return toResponse(entity);
    }

    private NotificationResponse toResponse(NotificationEntity entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .type(entity.getType().name())
                .channel(entity.getChannel().name())
                .recipient(entity.getRecipient())
                .subject(entity.getSubject())
                .body(entity.getBody())
                .status(entity.getStatus().name())
                .eventType(entity.getEventType())
                .eventId(entity.getEventId())
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .build();
    }
}
