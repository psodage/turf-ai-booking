package com.turfai.booking.repository;

import com.turfai.booking.entity.BookingHold;
import com.turfai.booking.entity.HoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingHoldRepository extends JpaRepository<BookingHold, UUID> {

    Optional<BookingHold> findByBookingId(UUID bookingId);

    /**
     * Used by 2-minute cleanup job to find active holds that have expired (ADR-005).
     */
    List<BookingHold> findByStatusAndExpiresAtBefore(HoldStatus status, Instant now);
}
