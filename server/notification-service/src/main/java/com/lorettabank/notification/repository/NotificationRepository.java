package com.lorettabank.notification.repository;

import com.lorettabank.notification.entity.NotificationEntity;
import com.lorettabank.notification.entity.NotificationStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Page<NotificationEntity> findByUserId(Long userId, Pageable pageable);

    List<NotificationEntity> findByStatus(NotificationStatus status);
}
