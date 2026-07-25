package com.turfai.booking.ai;

import com.turfai.booking.ai.tool.AiToolGateway;
import com.turfai.booking.ai.tool.ToolResult;
import com.turfai.booking.dto.response.DaySlotsResponse;
import com.turfai.booking.exception.SlotUnavailableException;
import com.turfai.booking.repository.BookingRepository;
import com.turfai.booking.service.BlockedSlotService;
import com.turfai.booking.service.BookingService;
import com.turfai.booking.service.PaymentService;
import com.turfai.booking.service.PricingService;
import com.turfai.booking.service.SlotService;
import com.turfai.booking.service.TurfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiToolGatewayTest {

    @Mock private SlotService slotService;
    @Mock private PricingService pricingService;
    @Mock private BookingService bookingService;
    @Mock private BlockedSlotService blockedSlotService;
    @Mock private TurfService turfService;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentService paymentService;
    @Mock private ReportService reportService;
    @Mock private WhatsAppService whatsAppService;

    private AiToolGateway aiToolGateway;

    @BeforeEach
    void setUp() {
        aiToolGateway = new AiToolGateway(slotService, pricingService, bookingService, blockedSlotService, turfService, bookingRepository, paymentService, reportService, whatsAppService);
    }

    @Test
    @DisplayName("checkAvailability tool should return successful ToolResult with DaySlotsResponse")
    void testCheckAvailabilitySuccess() {
        UUID turfId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);
        DaySlotsResponse response = DaySlotsResponse.builder().turfId(turfId).date(date).slots(List.of()).build();

        when(slotService.getAvailableSlots(turfId, date)).thenReturn(response);

        ToolResult result = aiToolGateway.checkAvailability(turfId, date);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(response);
    }

    @Test
    @DisplayName("createBookingHold tool should map domain exception to ToolResult error with suggestions")
    void testCreateBookingHoldErrorMapping() {
        UUID turfId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);

        when(bookingService.createBookingHold(any())).thenThrow(new SlotUnavailableException("Requested slot is unavailable."));

        ToolResult result = aiToolGateway.createBookingHold(turfId, customerId, date, LocalTime.of(18, 0), LocalTime.of(19, 0));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("SLOT_UNAVAILABLE");
        assertThat(result.getMessage()).contains("unavailable");
    }
}
