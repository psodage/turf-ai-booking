package com.turfai.booking.repository;

import com.turfai.booking.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * ADR-003: Booking to Payment is 1:N.
     */
    List<Payment> findByBookingId(UUID bookingId);

    /**
     * Unique lookup by Razorpay payment ID for webhook processing and deduplication.
     */
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
}
