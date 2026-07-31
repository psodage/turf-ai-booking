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
        log.info("Demo Mode: Scheduling fast 5-second auto-confirmation for booking ID: {}", bookingId);
        scheduler.schedule(() -> {
            try {
                log.info("Demo Mode: Executing auto-confirmation for booking ID: {}", bookingId);
                bookingService.confirmBooking(bookingId, "DEMO_AUTO_CONFIRM");
            } catch (Exception ex) {
                log.error("Demo Mode: Error during auto-confirmation for booking ID: {}", bookingId, ex);
            }
        }, 5, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
    }
}
