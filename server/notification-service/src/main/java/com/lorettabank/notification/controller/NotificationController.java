package com.lorettabank.notification.controller;

import com.lorettabank.notification.dto.NotificationResponse;
import com.lorettabank.notification.service.NotificationService;
import com.lorettabank.shared.dto.PagedResponse;
import com.lorettabank.shared.exception.ForbiddenException;
import com.lorettabank.shared.security.JwtConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> listNotifications(
            @PageableDefault(size = 20) Pageable pageable) {
        Long authenticatedUserId = getAuthenticatedUserId();
        PagedResponse<NotificationResponse> response =
                notificationService.getNotifications(authenticatedUserId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotification(@PathVariable Long id) {
        NotificationResponse response = notificationService.getNotification(id);
        if (!hasRole(JwtConstants.ROLE_ADMIN)
                && !hasRole(JwtConstants.ROLE_SUPPORT)
                && (response.getUserId() == null
                        || !response.getUserId().equals(getAuthenticatedUserId()))) {
            throw new ForbiddenException("You can only access your own notifications");
        }
        return ResponseEntity.ok(response);
    }

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
