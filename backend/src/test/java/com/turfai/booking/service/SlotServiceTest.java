package com.turfai.booking.service;

import com.turfai.booking.dto.response.DaySlotsResponse;
import com.turfai.booking.dto.response.SlotResponse;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.BusinessStatus;
import com.turfai.booking.entity.OperatingHours;
import com.turfai.booking.entity.PricingRule;
import com.turfai.booking.entity.PricingType;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.entity.TurfStatus;
import com.turfai.booking.entity.TurfType;
import com.turfai.booking.repository.BlockedSlotRepository;
import com.turfai.booking.repository.BookingHoldRepository;
import com.turfai.booking.repository.BookingRepository;
import com.turfai.booking.repository.OperatingHoursRepository;
import com.turfai.booking.repository.PricingRuleRepository;
import com.turfai.booking.repository.TurfRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class SlotServiceTest {

    @Mock private TurfRepository turfRepository;
    @Mock private OperatingHoursRepository operatingHoursRepository;
    @Mock private PricingRuleRepository pricingRuleRepository;
    @Mock private BlockedSlotRepository blockedSlotRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingHoldRepository bookingHoldRepository;

    private AutoCloseable autoCloseable;
    private SlotService slotService;
    private TurfService turfService;
    private OperatingHoursService operatingHoursService;
    private PricingService pricingService;

    private UUID turfId;
    private Turf testTurf;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        turfService = new TurfService(turfRepository);
        operatingHoursService = new OperatingHoursService(operatingHoursRepository);
        pricingService = new PricingService(pricingRuleRepository, operatingHoursService);
        slotService = new SlotService(turfService, operatingHoursService, pricingService, blockedSlotRepository, bookingRepository, bookingHoldRepository);

        turfId = UUID.randomUUID();
        Business business = Business.builder()
                .name("Green Pitch")
                .whatsappPhoneNumberId("PN_123")
                .timezone("Asia/Kolkata")
                .status(BusinessStatus.ACTIVE)
                .build();

        testTurf = Turf.builder()
                .business(business)
                .name("Main Turf")
                .type(TurfType.FIVE_A_SIDE)
                .status(TurfStatus.ACTIVE)
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (autoCloseable != null) {
            autoCloseable.close();
        }
    }

    @Test
    @DisplayName("Should generate 60-minute contiguous slots for opening hours 06:00 to 23:00")
    void testGetAvailableSlotsSuccess() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        int dayOfWeekIndex = operatingHoursService.toDayOfWeekIndex(tomorrow);

        OperatingHours opHours = OperatingHours.builder()
                .turf(testTurf)
                .dayOfWeek(dayOfWeekIndex)
                .openingTime(LocalTime.of(6, 0))
                .closingTime(LocalTime.of(23, 0))
                .isClosed(false)
                .build();

        PricingRule baseRule = PricingRule.builder()
                .turf(testTurf)
                .pricingType(PricingType.BASE)
                .amount(new BigDecimal("1000.00"))
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(testTurf));
        when(operatingHoursRepository.findByTurfIdAndDayOfWeek(turfId, dayOfWeekIndex)).thenReturn(Optional.of(opHours));
        when(pricingRuleRepository.findByTurfId(turfId)).thenReturn(List.of(baseRule));
        when(blockedSlotRepository.findByTurfIdAndDate(eq(turfId), any(LocalDate.class))).thenReturn(List.of());
        when(bookingRepository.findByTurfIdAndBookingDateAndStatusIn(eq(turfId), any(LocalDate.class), anyCollection())).thenReturn(List.of());

        DaySlotsResponse response = slotService.getAvailableSlots(turfId, tomorrow);

        assertThat(response.isClosed()).isFalse();
        assertThat(response.getSlots()).hasSize(17); // 6am to 11pm = 17 60-min slots
        assertThat(response.getSlots().get(0).getStartTime()).isEqualTo(LocalTime.of(6, 0));
        assertThat(response.getSlots().get(0).getEndTime()).isEqualTo(LocalTime.of(7, 0));
        assertThat(response.getSlots().get(0).getPrice()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(response.getSlots().get(0).isAvailable()).isTrue();
    }

    @Test
    @DisplayName("PEAK pricing rule should override BASE pricing rule")
    void testPeakPricingHierarchy() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        int dayOfWeekIndex = operatingHoursService.toDayOfWeekIndex(tomorrow);

        OperatingHours opHours = OperatingHours.builder()
                .turf(testTurf)
                .dayOfWeek(dayOfWeekIndex)
                .openingTime(LocalTime.of(6, 0))
                .closingTime(LocalTime.of(23, 0))
                .isClosed(false)
                .build();

        PricingRule baseRule = PricingRule.builder()
                .turf(testTurf)
                .pricingType(PricingType.BASE)
                .amount(new BigDecimal("1000.00"))
                .build();

        PricingRule peakRule = PricingRule.builder()
                .turf(testTurf)
                .pricingType(PricingType.PEAK)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(22, 0))
                .amount(new BigDecimal("1500.00"))
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(testTurf));
        when(operatingHoursRepository.findByTurfIdAndDayOfWeek(turfId, dayOfWeekIndex)).thenReturn(Optional.of(opHours));
        when(pricingRuleRepository.findByTurfId(turfId)).thenReturn(List.of(baseRule, peakRule));
        when(blockedSlotRepository.findByTurfIdAndDate(eq(turfId), any(LocalDate.class))).thenReturn(List.of());
        when(bookingRepository.findByTurfIdAndBookingDateAndStatusIn(eq(turfId), any(LocalDate.class), anyCollection())).thenReturn(List.of());

        DaySlotsResponse response = slotService.getAvailableSlots(turfId, tomorrow);

        // Find 18:00 - 19:00 slot
        SlotResponse peakSlot = response.getSlots().stream()
                .filter(s -> s.getStartTime().equals(LocalTime.of(18, 0)))
                .findFirst()
                .orElseThrow();

        assertThat(peakSlot.getPricingType()).isEqualTo(PricingType.PEAK);
        assertThat(peakSlot.getPrice()).isEqualTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("Regression: 06:00 to 23:00 last slot must end exactly at closing time without infinite loop")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testLastSlotEndingExactlyAtClosingTime() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        int dayOfWeekIndex = operatingHoursService.toDayOfWeekIndex(tomorrow);

        OperatingHours opHours = OperatingHours.builder()
                .turf(testTurf)
                .dayOfWeek(dayOfWeekIndex)
                .openingTime(LocalTime.of(6, 0))
                .closingTime(LocalTime.of(23, 0))
                .isClosed(false)
                .build();

        PricingRule baseRule = PricingRule.builder()
                .turf(testTurf)
                .pricingType(PricingType.BASE)
                .amount(new BigDecimal("1000.00"))
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(testTurf));
        when(operatingHoursRepository.findByTurfIdAndDayOfWeek(turfId, dayOfWeekIndex)).thenReturn(Optional.of(opHours));
        when(pricingRuleRepository.findByTurfId(turfId)).thenReturn(List.of(baseRule));
        when(blockedSlotRepository.findByTurfIdAndDate(eq(turfId), any(LocalDate.class))).thenReturn(List.of());
        when(bookingRepository.findByTurfIdAndBookingDateAndStatusIn(eq(turfId), any(LocalDate.class), anyCollection())).thenReturn(List.of());

        DaySlotsResponse response = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> slotService.getAvailableSlots(turfId, tomorrow));

        assertThat(response.getSlots()).hasSize(17);
        SlotResponse lastSlot = response.getSlots().get(response.getSlots().size() - 1);
        assertThat(lastSlot.getStartTime()).isEqualTo(LocalTime.of(22, 0));
        assertThat(lastSlot.getEndTime()).isEqualTo(LocalTime.of(23, 0));
    }

    @Test
    @DisplayName("Regression: Partial slot exceeding closing time should be omitted")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testSlotExceedingClosingTimeOmitted() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        int dayOfWeekIndex = operatingHoursService.toDayOfWeekIndex(tomorrow);

        OperatingHours opHours = OperatingHours.builder()
                .turf(testTurf)
                .dayOfWeek(dayOfWeekIndex)
                .openingTime(LocalTime.of(6, 0))
                .closingTime(LocalTime.of(22, 30))
                .isClosed(false)
                .build();

        PricingRule baseRule = PricingRule.builder()
                .turf(testTurf)
                .pricingType(PricingType.BASE)
                .amount(new BigDecimal("1000.00"))
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(testTurf));
        when(operatingHoursRepository.findByTurfIdAndDayOfWeek(turfId, dayOfWeekIndex)).thenReturn(Optional.of(opHours));
        when(pricingRuleRepository.findByTurfId(turfId)).thenReturn(List.of(baseRule));
        when(blockedSlotRepository.findByTurfIdAndDate(eq(turfId), any(LocalDate.class))).thenReturn(List.of());
        when(bookingRepository.findByTurfIdAndBookingDateAndStatusIn(eq(turfId), any(LocalDate.class), anyCollection())).thenReturn(List.of());

        DaySlotsResponse response = slotService.getAvailableSlots(turfId, tomorrow);

        assertThat(response.getSlots()).hasSize(16);
        SlotResponse lastSlot = response.getSlots().get(response.getSlots().size() - 1);
        assertThat(lastSlot.getStartTime()).isEqualTo(LocalTime.of(21, 0));
        assertThat(lastSlot.getEndTime()).isEqualTo(LocalTime.of(22, 0));
    }

    @Test
    @DisplayName("Regression: Zero-duration operating hours should return empty slots safely")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testZeroDurationOperatingHours() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        int dayOfWeekIndex = operatingHoursService.toDayOfWeekIndex(tomorrow);

        OperatingHours opHours = OperatingHours.builder()
                .turf(testTurf)
                .dayOfWeek(dayOfWeekIndex)
                .openingTime(LocalTime.of(10, 0))
                .closingTime(LocalTime.of(10, 0))
                .isClosed(false)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(testTurf));
        when(operatingHoursRepository.findByTurfIdAndDayOfWeek(turfId, dayOfWeekIndex)).thenReturn(Optional.of(opHours));

        DaySlotsResponse response = slotService.getAvailableSlots(turfId, tomorrow);

        assertThat(response.getSlots()).isEmpty();
    }

    @Test
    @DisplayName("Regression: Invalid operating hours (closing before opening) should return empty slots safely")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testInvalidOperatingHoursClosingBeforeOpening() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        int dayOfWeekIndex = operatingHoursService.toDayOfWeekIndex(tomorrow);

        OperatingHours opHours = OperatingHours.builder()
                .turf(testTurf)
                .dayOfWeek(dayOfWeekIndex)
                .openingTime(LocalTime.of(20, 0))
                .closingTime(LocalTime.of(10, 0))
                .isClosed(false)
                .build();

        when(turfRepository.findById(turfId)).thenReturn(Optional.of(testTurf));
        when(operatingHoursRepository.findByTurfIdAndDayOfWeek(turfId, dayOfWeekIndex)).thenReturn(Optional.of(opHours));

        DaySlotsResponse response = slotService.getAvailableSlots(turfId, tomorrow);

        assertThat(response.getSlots()).isEmpty();
    }
}
