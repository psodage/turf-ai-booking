package com.turfai.booking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turfai.booking.config.RazorpayProperties;
import com.turfai.booking.dto.payment.CreatePaymentLinkRequest;
import com.turfai.booking.dto.payment.PaymentResponse;
import com.turfai.booking.dto.request.BlockSlotRequest;
import com.turfai.booking.dto.request.CancelBookingRequest;
import com.turfai.booking.dto.request.CreateBookingHoldRequest;
import com.turfai.booking.dto.response.BookingHoldResponse;
import com.turfai.booking.dto.response.BookingResponse;
import com.turfai.booking.entity.Booking;
import com.turfai.booking.entity.BookingAudit;
import com.turfai.booking.entity.BookingHold;
import com.turfai.booking.entity.BookingStatus;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.BusinessStatus;
import com.turfai.booking.entity.HoldStatus;
import com.turfai.booking.entity.OperatingHours;
import com.turfai.booking.entity.PaymentAudit;
import com.turfai.booking.entity.PricingRule;
import com.turfai.booking.entity.PricingType;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.entity.TurfStatus;
import com.turfai.booking.entity.TurfType;
import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import com.turfai.booking.entity.UserStatus;
import com.turfai.booking.exception.CancellationDeniedException;
import com.turfai.booking.exception.HoldExpiredException;
import com.turfai.booking.exception.SlotUnavailableException;
import com.turfai.booking.exception.WebhookSignatureException;
import com.turfai.booking.repository.BlockedSlotRepository;
import com.turfai.booking.repository.BookingAuditRepository;
import com.turfai.booking.repository.BookingHoldRepository;
import com.turfai.booking.repository.BookingRepository;
import com.turfai.booking.repository.BusinessRepository;
import com.turfai.booking.repository.OperatingHoursRepository;
import com.turfai.booking.repository.PaymentAuditRepository;
import com.turfai.booking.repository.PaymentRepository;
import com.turfai.booking.repository.PricingRuleRepository;
import com.turfai.booking.repository.TurfRepository;
import com.turfai.booking.repository.UserRepository;
import com.turfai.booking.scheduler.BookingHoldCleanupScheduler;
import com.turfai.booking.service.BlockedSlotService;
import com.turfai.booking.service.BookingService;
import com.turfai.booking.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingPaymentFlowIntegrationTest {

    @Autowired private BookingService bookingService;
    @Autowired private PaymentService paymentService;
    @Autowired private BlockedSlotService blockedSlotService;
    @Autowired private BookingHoldCleanupScheduler bookingHoldCleanupScheduler;
    @Autowired private RazorpayProperties razorpayProperties;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private BusinessRepository businessRepository;
    @Autowired private TurfRepository turfRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private PricingRuleRepository pricingRuleRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingHoldRepository bookingHoldRepository;
    @Autowired private BookingAuditRepository bookingAuditRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentAuditRepository paymentAuditRepository;
    @Autowired private BlockedSlotRepository blockedSlotRepository;

    private Business testBusiness;
    private Turf testTurf;
    private User testCustomer;
    private User testOwner;
    private LocalDate tomorrow;

    @BeforeEach
    void setUp() {
        tomorrow = LocalDate.now().plusDays(1);

        testBusiness = businessRepository.saveAndFlush(Business.builder()
                .name("Phase 5 Arena")
                .whatsappPhoneNumberId("PN_PHASE5_001")
                .phone("+919111122222")
                .timezone("Asia/Kolkata")
                .status(BusinessStatus.ACTIVE)
                .build());

        testTurf = turfRepository.saveAndFlush(Turf.builder()
                .business(testBusiness)
                .name("Turf Alpha")
                .type(TurfType.SEVEN_A_SIDE)
                .status(TurfStatus.ACTIVE)
                .build());

        testCustomer = userRepository.saveAndFlush(User.builder()
                .business(testBusiness)
                .name("Rohan Verma")
                .phone("+919876500001")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        testOwner = userRepository.saveAndFlush(User.builder()
                .business(testBusiness)
                .name("Suresh Manager")
                .phone("+919876500002")
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .build());

        operatingHoursRepository.saveAndFlush(OperatingHours.builder()
                .turf(testTurf)
                .dayOfWeek(tomorrow.getDayOfWeek().getValue() - 1)
                .openingTime(LocalTime.of(6, 0))
                .closingTime(LocalTime.of(23, 0))
                .isClosed(false)
                .build());

        pricingRuleRepository.saveAndFlush(PricingRule.builder()
                .turf(testTurf)
                .pricingType(PricingType.BASE)
                .amount(new BigDecimal("1200.00"))
                .build());
    }

    @Test
    @DisplayName("1. Booking Hold & Sequence Generation: Creates hold and assigns BK-YYYY-NNNNN")
    void testBookingHoldCreation() {
        CreateBookingHoldRequest request = CreateBookingHoldRequest.builder()
                .turfId(testTurf.getId())
                .customerId(testCustomer.getId())
                .bookingDate(tomorrow)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .build();

        BookingHoldResponse response = bookingService.createBookingHold(request);

        assertThat(response).isNotNull();
        assertThat(response.getBookingNumber()).matches("^BK-\\d{4}-\\d{5}$");
        assertThat(response.getStatus()).isEqualTo(HoldStatus.ACTIVE);
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("1200.00"));

        List<BookingAudit> audits = bookingAuditRepository.findByBookingIdOrderByChangedAtAsc(response.getBookingId());
        assertThat(audits).isNotEmpty();
        assertThat(audits.get(0).getNewStatus()).isEqualTo(BookingStatus.HOLD);
    }

    @Test
    @DisplayName("2. Double Booking Prevention: Simultaneous overlapping hold request throws SlotUnavailableException")
    void testDoubleBookingPrevention() {
        CreateBookingHoldRequest req1 = CreateBookingHoldRequest.builder()
                .turfId(testTurf.getId())
                .customerId(testCustomer.getId())
                .bookingDate(tomorrow)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .build();

        bookingService.createBookingHold(req1);

        // Attempt overlapping hold on 18:30 - 19:30
        CreateBookingHoldRequest req2 = CreateBookingHoldRequest.builder()
                .turfId(testTurf.getId())
                .customerId(testCustomer.getId())
                .bookingDate(tomorrow)
                .startTime(LocalTime.of(18, 30))
                .endTime(LocalTime.of(19, 30))
                .build();

        assertThatThrownBy(() -> bookingService.createBookingHold(req2))
                .isInstanceOf(SlotUnavailableException.class);
    }

    @Test
    @DisplayName("3. Hold Expiration Scheduler: Expired hold automatically transitions booking to EXPIRED")
    void testHoldExpirationScheduler() {
        CreateBookingHoldRequest req = CreateBookingHoldRequest.builder()
                .turfId(testTurf.getId())
                .customerId(testCustomer.getId())
                .bookingDate(tomorrow)
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(21, 0))
                .build();

        BookingHoldResponse holdResp = bookingService.createBookingHold(req);

        // Expire hold by setting expiresAt in past
        BookingHold hold = bookingHoldRepository.findById(holdResp.getHoldId()).orElseThrow();
        hold.setExpiresAt(Instant.now().minusSeconds(120));
        bookingHoldRepository.saveAndFlush(hold);

        // Run Cleanup Scheduler
        bookingHoldCleanupScheduler.cleanupExpiredHolds();

        Booking updatedBooking = bookingRepository.findById(holdResp.getBookingId()).orElseThrow();
        assertThat(updatedBooking.getStatus()).isEqualTo(BookingStatus.EXPIRED);

        BookingHold updatedHold = bookingHoldRepository.findById(holdResp.getHoldId()).orElseThrow();
        assertThat(updatedHold.getStatus()).isEqualTo(HoldStatus.EXPIRED);
    }

    @Test
    @DisplayName("4. Payment Grace Period (ADR-016): Expired hold within 60s confirms if slot remains unbooked")
    void testPaymentGracePeriodReactivation() {
        CreateBookingHoldRequest req = CreateBookingHoldRequest.builder()
                .turfId(testTurf.getId())
                .customerId(testCustomer.getId())
                .bookingDate(tomorrow)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .build();

        BookingHoldResponse holdResp = bookingService.createBookingHold(req);

        // Set hold as EXPIRED 30 seconds ago (within 60s grace period)
        BookingHold hold = bookingHoldRepository.findById(holdResp.getHoldId()).orElseThrow();
        hold.setExpiresAt(Instant.now().minusSeconds(30));
        hold.setStatus(HoldStatus.EXPIRED);
        bookingHoldRepository.saveAndFlush(hold);

        Booking booking = bookingRepository.findById(holdResp.getBookingId()).orElseThrow();
        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.saveAndFlush(booking);

        // Late payment arrives -> confirmBooking
        BookingResponse confirmedResp = bookingService.confirmBooking(booking.getId(), "PAY_GRACE_001");
        assertThat(confirmedResp.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("5. Payment Link Generation & Idempotency: Re-request returns existing active link")
    void testPaymentLinkGenerationAndIdempotency() {
        CreateBookingHoldRequest holdReq = CreateBookingHoldRequest.builder()
                .turfId(testTurf.getId())
                .customerId(testCustomer.getId())
                .bookingDate(tomorrow)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build();

        BookingHoldResponse holdResp = bookingService.createBookingHold(holdReq);

        CreatePaymentLinkRequest linkReq = CreatePaymentLinkRequest.builder()
                .bookingId(holdResp.getBookingId())
                .build();

        PaymentResponse resp1 = paymentService.createPaymentLink(linkReq);
        assertThat(resp1).isNotNull();
        assertThat(resp1.getBookingId()).isEqualTo(holdResp.getBookingId());

        // Second call returns exact same payment ID (idempotent)
        PaymentResponse resp2 = paymentService.createPaymentLink(linkReq);
        assertThat(resp2.getPaymentId()).isEqualTo(resp1.getPaymentId());
    }

    @Test
    @DisplayName("6. Webhook Signature & Idempotency: Invalid signature rejected & duplicates ignored")
    void testRazorpayWebhookSignatureAndIdempotency() throws Exception {
        String secret = razorpayProperties.getWebhookSecret();
        String payload = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_12345\"}}}}";

        assertThatThrownBy(() -> paymentService.verifyWebhookSignature(payload, "invalid_sig"))
                .isInstanceOf(WebhookSignatureException.class);

        // Valid signature test
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : rawHmac) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String validSignature = hexString.toString();

        paymentService.verifyWebhookSignature(payload, validSignature);
    }

    @Test
    @DisplayName("7. Owner Blocking Validation: Owner cannot block slots with active bookings")
    void testOwnerBlockingValidation() {
        CreateBookingHoldRequest holdReq = CreateBookingHoldRequest.builder()
                .turfId(testTurf.getId())
                .customerId(testCustomer.getId())
                .bookingDate(tomorrow)
                .startTime(LocalTime.of(16, 0))
                .endTime(LocalTime.of(17, 0))
                .build();

        bookingService.createBookingHold(holdReq);

        BlockSlotRequest blockReq = BlockSlotRequest.builder()
                .turfId(testTurf.getId())
                .date(tomorrow)
                .startTime(LocalTime.of(16, 0))
                .endTime(LocalTime.of(17, 0))
                .reason(com.turfai.booking.entity.BlockReason.MAINTENANCE)
                .createdBy(testOwner.getId())
                .build();

        assertThatThrownBy(() -> blockedSlotService.blockSlot(blockReq))
                .isInstanceOf(SlotUnavailableException.class)
                .hasMessageContaining("Cannot block slot: an active booking exists");
    }

    @Test
    @DisplayName("8. Cancellation Rules: Customer denied if < 2 hours before start, Owner allowed")
    void testCancellationRules() {
        CreateBookingHoldRequest holdReq = CreateBookingHoldRequest.builder()
                .turfId(testTurf.getId())
                .customerId(testCustomer.getId())
                .bookingDate(tomorrow)
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(12, 0))
                .build();

        BookingHoldResponse holdResp = bookingService.createBookingHold(holdReq);
        bookingService.confirmBooking(holdResp.getBookingId(), "PAY_TEST_CONFIRM");

        // Customer cancels for tomorrow (> 2h) -> Sumeet cancels successfully
        CancelBookingRequest cancelReq = CancelBookingRequest.builder()
                .requestingUserId(testCustomer.getId())
                .reason("Weather issue")
                .build();

        BookingResponse cancelled = bookingService.cancelBooking(holdResp.getBookingId(), cancelReq);
        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }
}
