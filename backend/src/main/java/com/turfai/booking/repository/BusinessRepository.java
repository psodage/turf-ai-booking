package com.turfai.booking.repository;

import com.turfai.booking.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    /**
     * Finds a business by its dedicated WhatsApp Business phone number ID (ADR-006).
     */
    Optional<Business> findByWhatsappPhoneNumberId(String whatsappPhoneNumberId);
}
