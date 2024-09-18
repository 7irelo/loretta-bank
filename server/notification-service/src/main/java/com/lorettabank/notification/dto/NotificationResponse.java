package com.lorettabank.notification.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long userId;
    private String type;
    private String channel;
    private String recipient;
    private String subject;
    private String body;
    private String status;
    private String eventType;
    private String eventId;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
