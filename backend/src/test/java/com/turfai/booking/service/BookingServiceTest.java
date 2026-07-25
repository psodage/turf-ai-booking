package com.turfai.booking.service;

import com.turfai.booking.dto.request.CancelBookingRequest;
import com.turfai.booking.entity.Booking;
import com.turfai.booking.entity.BookingStatus;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.BusinessStatus;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.entity.TurfStatus;
import com.turfai.booking.entity.TurfType;
import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import com.turfai.booking.entity.UserStatus;
import com.turfai.booking.exception.CancellationDeniedException;
import com.turfai.booking.repository.BookingAuditRepository;
import com.turfai.booking.repository.BookingHoldRepository;
import com.turfai.booking.repository.BookingRepository;
import com.turfai.booking.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private TurfService turfService;
    @Mock private OperatingHoursService operatingHoursService;
    @Mock private PricingService pricingService;
    @Mock private SlotService slotService;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingHoldRepository bookingHoldRepository;
    @Mock private BookingAuditRepository bookingAuditRepository;
    @Mock private UserRepository userRepository;
    @Mock private EntityManager entityManager;

    private BookingService bookingService;

    private Business testBusiness;
    private Turf testTurf;
    private User testCustomer;
    private User testOwner;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(turfService, operatingHoursService, pricingService, slotService, bookingRepository, bookingHoldRepository, bookingAuditRepository, userRepository);

        testBusiness = Business.builder()
                .name("Green Pitch")
                .whatsappPhoneNumberId("PN_123")
                .timezone("Asia/Kolkata")
                .status(BusinessStatus.ACTIVE)
                .build();
        testBusiness.setId(UUID.randomUUID());

        testTurf = Turf.builder()
                .business(testBusiness)
                .name("Main Turf")
                .type(TurfType.FIVE_A_SIDE)
                .status(TurfStatus.ACTIVE)
                .build();
        testTurf.setId(UUID.randomUUID());

        testCustomer = User.builder()
                .name("Amit Kumar")
                .phone("+919876543210")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
        testCustomer.setId(UUID.randomUUID());

        testOwner = User.builder()
                .name("Rajesh Owner")
                .phone("+919876543211")
                .role(UserRole.OWNER)
                .business(testBusiness)
                .status(UserStatus.ACTIVE)
                .build();
        testOwner.setId(UUID.randomUUID());

        bookingId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Customer cancellation at least 2 hours before start should succeed")
    void testCustomerCancellationSuccess() {
        ZonedDateTime nowInIST = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        LocalDate bookingDate = nowInIST.toLocalDate().plusDays(2); // 2 days in future (> 2h)

        Booking booking = Booking.builder()
                .bookingNumber("BK-2026-00001")
                .business(testBusiness)
                .turf(testTurf)
                .customer(testCustomer)
                .bookingDate(bookingDate)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .price(new BigDecimal("1000.00"))
                .status(BookingStatus.CONFIRMED)
                .build();
        booking.setId(bookingId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        CancelBookingRequest request = CancelBookingRequest.builder()
                .requestingUserId(testCustomer.getId())
                .reason("Change of plans")
                .build();

        var response = bookingService.cancelBooking(bookingId, request);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.getCancelledBy()).isEqualTo(testCustomer.getId());
    }

    @Test
    @DisplayName("Customer cancellation less than 2 hours before start should throw CancellationDeniedException")
    void testCustomerCancellationDeniedUnder2Hours() {
        ZonedDateTime nowInIST = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        LocalDate bookingDate = nowInIST.toLocalDate();
        LocalTime slotStartTime = nowInIST.toLocalTime().plusMinutes(30); // Only 30 minutes away (< 2h)

        Booking booking = Booking.builder()
                .bookingNumber("BK-2026-00002")
                .business(testBusiness)
                .turf(testTurf)
                .customer(testCustomer)
                .bookingDate(bookingDate)
                .startTime(slotStartTime)
                .endTime(slotStartTime.plusHours(1))
                .price(new BigDecimal("1000.00"))
                .status(BookingStatus.CONFIRMED)
                .build();
        booking.setId(bookingId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));

        CancelBookingRequest request = CancelBookingRequest.builder()
                .requestingUserId(testCustomer.getId())
                .reason("Late cancellation")
                .build();

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, request))
                .isInstanceOf(CancellationDeniedException.class)
                .hasMessageContaining("at least 2 hours before start time");
    }

    @Test
    @DisplayName("Owner cancellation should succeed even if less than 2 hours before start time")
    void testOwnerCancellationOverride() {
        ZonedDateTime nowInIST = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        LocalDate bookingDate = nowInIST.toLocalDate();
        LocalTime slotStartTime = nowInIST.toLocalTime().plusMinutes(30); // 30 mins away

        Booking booking = Booking.builder()
                .bookingNumber("BK-2026-00003")
                .business(testBusiness)
                .turf(testTurf)
                .customer(testCustomer)
                .bookingDate(bookingDate)
                .startTime(slotStartTime)
                .endTime(slotStartTime.plusHours(1))
                .price(new BigDecimal("1000.00"))
                .status(BookingStatus.CONFIRMED)
                .build();
        booking.setId(bookingId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userRepository.findById(testOwner.getId())).thenReturn(Optional.of(testOwner));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        CancelBookingRequest request = CancelBookingRequest.builder()
                .requestingUserId(testOwner.getId())
                .reason("Owner emergency maintenance")
                .build();

        var response = bookingService.cancelBooking(bookingId, request);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.getCancelledBy()).isEqualTo(testOwner.getId());
    }
}
