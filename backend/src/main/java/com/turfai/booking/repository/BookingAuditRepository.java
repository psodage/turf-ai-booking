package com.turfai.booking.repository;

import com.turfai.booking.entity.BookingAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingAuditRepository extends JpaRepository<BookingAudit, UUID> {

    List<BookingAudit> findByBookingIdOrderByChangedAtAsc(UUID bookingId);
}
