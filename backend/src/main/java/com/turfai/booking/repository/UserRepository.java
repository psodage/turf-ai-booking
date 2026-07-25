package com.turfai.booking.repository;

import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import com.turfai.booking.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by unique WhatsApp phone number.
     */
    Optional<User> findByPhone(String phone);

    /**
     * Finds an active user by phone number.
     */
    Optional<User> findByPhoneAndStatus(String phone, UserStatus status);

    /**
     * Finds all staff members (OWNER/MANAGER) for a business.
     */
    List<User> findByBusinessIdAndRole(UUID businessId, UserRole role);
}
