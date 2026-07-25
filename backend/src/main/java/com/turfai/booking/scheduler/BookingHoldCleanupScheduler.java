package com.turfai.booking.scheduler;

import com.turfai.booking.entity.Booking;
import com.turfai.booking.entity.BookingAudit;
import com.turfai.booking.entity.BookingHold;
import com.turfai.booking.entity.BookingStatus;
import com.turfai.booking.entity.HoldStatus;
import com.turfai.booking.repository.BookingAuditRepository;
import com.turfai.booking.repository.BookingHoldRepository;
import com.turfai.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Background scheduler running every 2 minutes (ADR-005) to clean up expired booking holds.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingHoldCleanupScheduler {

    private final BookingHoldRepository bookingHoldRepository;
    private final BookingRepository bookingRepository;
    private final BookingAuditRepository bookingAuditRepository;

    @Scheduled(cron = "${HOLD_CLEANUP_CRON:0 */2 * * * *}")
    @Transactional
    public void cleanupExpiredHolds() {
        Instant nowUtc = Instant.now();
        List<BookingHold> expiredHolds = bookingHoldRepository.findByStatusAndExpiresAtBefore(HoldStatus.ACTIVE, nowUtc);

        if (expiredHolds.isEmpty()) {
            return;
        }

        log.info("Found {} active booking holds expired prior to {}", expiredHolds.size(), nowUtc);

        for (BookingHold hold : expiredHolds) {
            hold.setStatus(HoldStatus.EXPIRED);
            bookingHoldRepository.save(hold);

            Booking booking = hold.getBooking();
            if (booking.getStatus() == BookingStatus.HOLD || booking.getStatus() == BookingStatus.PAYMENT_PENDING) {
                BookingStatus oldStatus = booking.getStatus();
                booking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);

                BookingAudit audit = BookingAudit.builder()
                        .booking(booking)
                        .oldStatus(oldStatus)
                        .newStatus(BookingStatus.EXPIRED)
                        .reason("Hold timer expired (automatic 2-min cleanup job)")
                        .build();
                bookingAuditRepository.save(audit);

                log.debug("Booking {} marked EXPIRED by cleanup job", booking.getBookingNumber());
            }
        }
    }
}
