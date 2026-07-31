package com.turfai.booking.scheduler;

import com.turfai.booking.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class DemoAutoConfirmSchedulerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private DemoAutoConfirmScheduler scheduler;

    @Test
    @DisplayName("scheduleAutoConfirmation should accept bookingId and schedule task")
    void testScheduleAutoConfirmation() {
        UUID bookingId = UUID.randomUUID();
        scheduler.scheduleAutoConfirmation(bookingId);
        scheduler.destroy();
    }
}
