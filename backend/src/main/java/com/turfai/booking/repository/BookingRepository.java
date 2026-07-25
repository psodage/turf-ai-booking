package com.turfai.booking.repository;

import com.turfai.booking.entity.Booking;
import com.turfai.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByBookingNumber(String bookingNumber);

    @Query("SELECT b FROM Booking b JOIN FETCH b.turf JOIN FETCH b.customer JOIN FETCH b.business WHERE b.bookingNumber = :bookingNumber")
    Optional<Booking> findByBookingNumberWithDetails(@Param("bookingNumber") String bookingNumber);

    /**
     * Finds active bookings for conflict checking (status IN HOLD, PAYMENT_PENDING, CONFIRMED).
     */
    List<Booking> findByTurfIdAndBookingDateAndStatusIn(UUID turfId, LocalDate bookingDate, Collection<BookingStatus> statuses);

    /**
     * Finds bookings for a customer.
     */
    List<Booking> findByCustomerId(UUID customerId);

    List<Booking> findByCustomerIdAndStatus(UUID customerId, BookingStatus status);

    /**
     * Business bookings for dashboard/owner view on a specific date.
     */
    List<Booking> findByBusinessIdAndBookingDate(UUID businessId, LocalDate bookingDate);

    /**
     * Business bookings for report generation within date range.
     */
    List<Booking> findByBusinessIdAndBookingDateBetween(UUID businessId, LocalDate startDate, LocalDate endDate);

    /**
     * Sequence query for booking number generation.
     */
    @Query(value = "SELECT NEXTVAL('booking_number_seq')", nativeQuery = true)
    Long getNextBookingSequenceValue();
}
