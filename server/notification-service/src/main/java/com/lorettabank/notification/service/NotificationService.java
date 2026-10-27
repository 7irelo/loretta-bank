package com.lorettabank.notification.service;

import com.lorettabank.notification.dto.NotificationResponse;
import com.lorettabank.notification.entity.CustomerDirectoryEntry;
import com.lorettabank.notification.entity.NotificationChannel;
import com.lorettabank.notification.entity.NotificationEntity;
import com.lorettabank.notification.entity.NotificationStatus;
import com.lorettabank.notification.entity.NotificationType;
import com.lorettabank.notification.repository.CustomerDirectoryRepository;
import com.lorettabank.notification.repository.NotificationRepository;
import com.lorettabank.shared.dto.PagedResponse;
import com.lorettabank.shared.exception.ForbiddenException;
import com.lorettabank.shared.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomerDirectoryRepository directoryRepository;
    private final EmailSender emailSender;

    @Transactional
    public void rememberCustomer(Long customerId, Long userId, String email, String firstName) {
        directoryRepository.save(
                CustomerDirectoryEntry.builder()
                        .customerId(customerId)
                        .userId(userId)
                        .email(email)
                        .firstName(firstName)
                        .build());
    }

    /**
     * Notifies the user who owns {@code customerId}. Throws if the customer is not known yet so the
     * Kafka error handler retries; the CUSTOMER_CREATED event may still be in flight.
     */
    @Transactional
    public void notifyCustomer(
            Long customerId,
            NotificationType type,
            String subject,
            String body,
            String eventType,
            String eventId) {
        if (customerId == null) {
            return;
        }
        CustomerDirectoryEntry customer =
                directoryRepository
                        .findById(customerId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Customer "
                                                        + customerId
                                                        + " not in directory yet"));

        if (notificationRepository.existsByEventIdAndUserId(eventId, customer.getUserId())) {
            log.debug("Notification for event {} already recorded", eventId);
            return;
        }

        NotificationEntity notification =
                notificationRepository.save(
                        NotificationEntity.builder()
                                .userId(customer.getUserId())
                                .type(type)
                                .channel(NotificationChannel.IN_APP)
                                .recipient(customer.getEmail())
                                .subject(subject)
                                .body(body)
                                .status(NotificationStatus.SENT)
                                .sentAt(LocalDateTime.now())
                                .eventType(eventType)
                                .eventId(eventId)
                                .build());
        emailSender.send(customer.getEmail(), subject, body);
        log.info(
                "Notification {} ({}) for user {}",
                notification.getId(),
                type,
                customer.getUserId());
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getNotifications(Long userId, int page, int size) {
        Page<NotificationEntity> result =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(
                        userId,
                        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return PagedResponse.<NotificationResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotification(Long id, Long userId, boolean staff) {
        NotificationEntity entity = find(id);
        if (!staff && !userId.equals(entity.getUserId())) {
            throw new ResourceNotFoundException("Notification not found");
        }
        return toResponse(entity);
    }

    @Transactional
    public NotificationResponse markRead(Long id, Long userId) {
        NotificationEntity entity = find(id);
        if (!userId.equals(entity.getUserId())) {
            throw new ForbiddenException("You can only update your own notifications");
        }
        if (entity.getReadAt() == null) {
            entity.setReadAt(LocalDateTime.now());
        }
        return toResponse(entity);
    }

    @Transactional
    public int markAllRead(Long userId) {
        int[] count = {0};
        notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 500))
                .forEach(
                        n -> {
                            if (n.getReadAt() == null) {
                                n.setReadAt(LocalDateTime.now());
                                count[0]++;
                            }
                        });
        return count[0];
    }

    private NotificationEntity find(Long id) {
        return notificationRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    }

    private NotificationResponse toResponse(NotificationEntity entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .type(entity.getType().name())
                .channel(entity.getChannel().name())
                .subject(entity.getSubject())
                .body(entity.getBody())
                .status(entity.getStatus().name())
                .eventType(entity.getEventType())
                .eventId(entity.getEventId())
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .readAt(entity.getReadAt())
                .build();
    }
}
