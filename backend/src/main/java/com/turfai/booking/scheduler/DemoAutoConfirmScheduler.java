package com.turfai.booking.scheduler;

import com.turfai.booking.service.BookingService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Temporary Scheduler for Client Demo:
 * 30 seconds after Booking Hold is Created, automatically confirms the booking
 * and triggers confirmation message with time, date, and booking ID to the user via WhatsApp.
 */
@Slf4j
@Component
public class DemoAutoConfirmScheduler {

    private final BookingService bookingService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public DemoAutoConfirmScheduler(@Lazy BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public void scheduleAutoConfirmation(UUID bookingId) {
        log.info("Demo Mode: Scheduling 30-second auto-confirmation for booking ID: {}", bookingId);
        scheduler.schedule(() -> {
            int maxRetries = 3;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    log.info("Demo Mode: Executing auto-confirmation for booking ID: {} (attempt {}/{})", bookingId, attempt, maxRetries);
                    bookingService.confirmBooking(bookingId, "DEMO_AUTO_CONFIRM");
                    log.info("Demo Mode: Auto-confirmation succeeded for booking ID: {}", bookingId);
                    return;
                } catch (Exception ex) {
                    if (attempt < maxRetries) {
                        long backoffMs = attempt * 2000L;
                        log.warn("Demo Mode: Attempt {}/{} failed for booking ID {}: {}. Retrying in {}ms...",
                                attempt, maxRetries, bookingId, ex.getMessage(), backoffMs);
                        try {
                            Thread.sleep(backoffMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.error("Demo Mode: Retry interrupted for booking ID: {}", bookingId);
                            return;
                        }
                    } else {
                        log.error("Demo Mode: All {} attempts exhausted for booking ID: {}", maxRetries, bookingId, ex);
                    }
                }
            }
        }, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
    }
}
