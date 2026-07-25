package com.turfai.booking.repository;

import com.turfai.booking.entity.Notification;
import com.turfai.booking.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByUserId(UUID userId);
}
