package com.lorettabank.notification.repository;

import com.lorettabank.notification.entity.CustomerDirectoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerDirectoryRepository extends JpaRepository<CustomerDirectoryEntry, Long> {}
